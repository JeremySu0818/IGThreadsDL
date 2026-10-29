package com.jeremysu0818.igthreadsdl.data.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord
import com.jeremysu0818.igthreadsdl.domain.download.DownloadRepository
import com.jeremysu0818.igthreadsdl.domain.download.DownloadStatus
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import com.jeremysu0818.igthreadsdl.domain.model.MediaItem
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.model.MediaManifest
import com.jeremysu0818.igthreadsdl.domain.model.MediaPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

import com.jeremysu0818.igthreadsdl.i18n.AppLanguage
import com.jeremysu0818.igthreadsdl.i18n.AppStrings
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager

class AndroidDownloadRepository(
    context: Context,
) : DownloadRepository {
    private val appContext = context.applicationContext
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val preferences =
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val imageClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()
    private val imageCalls = ConcurrentHashMap<Long, Call>()
    private val recordLock = Any()
    private val initialRecords = loadRecords().map { record ->
        when {
            record.managerId < 0 && record.isActive ->
                record.copy(status = DownloadStatus.FAILED, statusMessage = strings.downloadStatusFailedFileErr)
            record.status == DownloadStatus.FAILED && record.statusMessage != null ->
                record.copy(statusMessage = cleanLegacyDownloadFailure(
                    record.statusMessage,
                    AppLanguage.supportedLanguages.map { LanguageManager.getStrings(it) },
                    strings,
                ))
            else -> record
        }
    }
    private val localId = AtomicLong(
        (initialRecords.minOfOrNull { it.managerId } ?: 0L)
            .coerceAtMost(0L) - 1L,
    )
    private val _records = MutableStateFlow(initialRecords)
    override val records: StateFlow<List<DownloadRecord>> = _records.asStateFlow()

    private val strings: AppStrings
        get() = LanguageManager.getStrings(LanguageManager.getSavedLanguage(appContext))

    private val completionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                scope.launch { refresh() }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(
            appContext,
            completionReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch {
            while (isActive) {
                refresh()
                delay(if (_records.value.any { it.isActive }) 1_000 else 4_000)
            }
        }
    }

    override suspend fun enqueue(
        manifest: MediaManifest,
        items: List<MediaItem>,
    ): List<DownloadRecord> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()
        val occupied = currentOccupiedFilenames().toMutableSet()
        val created = items.map { item ->
            val filename = uniqueFilename(item.filename, occupied)
            occupied += filename.lowercase(Locale.US)
            if (item.isHls) {
                failedRecord(
                    manifest = manifest,
                    item = item,
                    filename = filename,
                    message = strings.downloadHlsNotSupported,
                )
            } else {
                enqueueOne(manifest, item, filename)
            }
        }
        updateRecords { it + created }
        created.filter { it.managerId < 0 && it.isActive }.forEach { record ->
            scope.launch { downloadImage(record) }
        }
        created
    }

    override suspend fun cancel(managerId: Long) = withContext(Dispatchers.IO) {
        val cancelled = synchronized(recordLock) {
            if (_records.value.none { it.managerId == managerId && it.isActive }) {
                false
            } else {
                replaceRecord(managerId) {
                    it.copy(
                        status = DownloadStatus.CANCELLED,
                        statusMessage = strings.downloadMsgCancelled,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                true
            }
        }
        if (cancelled) {
            if (managerId > 0) downloadManager.remove(managerId)
            imageCalls.remove(managerId)?.cancel()
        }
    }

    override suspend fun retry(managerId: Long): DownloadRecord? = withContext(Dispatchers.IO) {
        val previous = _records.value.firstOrNull { it.managerId == managerId }
            ?: return@withContext null
        val item = MediaItem(
            id = previous.mediaId,
            type = previous.mediaType,
            downloadUrl = previous.downloadUrl,
            thumbnailUrl = previous.thumbnailUrl,
            width = null,
            height = null,
            durationMs = null,
            filename = previous.filename,
            contentLength = previous.totalBytes,
            mimeType = previous.mimeType,
            requestHeaders = previous.requestHeaders,
        )
        val manifest = MediaManifest(
            platform = previous.platform,
            type = if (previous.mediaType == MediaItemType.VIDEO) {
                ManifestType.VIDEO
            } else {
                ManifestType.PHOTO
            },
            author = previous.author,
            sourceUrl = previous.sourceUrl,
            title = null,
            caption = null,
            thumbnailUrl = previous.thumbnailUrl,
            items = listOf(item),
        )
        enqueue(manifest, listOf(item)).firstOrNull()
    }

    override suspend fun delete(managerId: Long): Boolean = withContext(Dispatchers.IO) {
        val record = synchronized(recordLock) {
            _records.value.firstOrNull { it.managerId == managerId }?.also {
                updateRecords { all -> all.filterNot { it.managerId == managerId } }
            }
        } ?: return@withContext false
        imageCalls.remove(managerId)?.cancel()
        val removed = if (record.managerId > 0) {
            runCatching { downloadManager.remove(record.managerId) >= 0 }.getOrDefault(false)
        } else {
            record.localUri?.let { uri ->
                runCatching {
                    appContext.contentResolver.delete(Uri.parse(uri), null, null) > 0
                }.getOrDefault(false)
            } ?: true
        }
        removed
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val managerIds = _records.value.map { it.managerId }.filter { it > 0 }.distinct()
        if (managerIds.isEmpty()) return@withContext
        val updates = mutableMapOf<Long, DownloadSnapshot>()
        val query = DownloadManager.Query().setFilterById(*managerIds.toLongArray())
        runCatching {
            downloadManager.query(query)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val snapshot = cursor.toSnapshot()
                    updates[snapshot.managerId] = snapshot
                }
            }
        }
        if (updates.isEmpty()) return@withContext

        val now = System.currentTimeMillis()
        updateRecords { current -> current.map { record ->
            if (record.status == DownloadStatus.CANCELLED) return@map record
            val snapshot = updates[record.managerId] ?: return@map record
            val uri = if (snapshot.status == DownloadStatus.SUCCEEDED) {
                runCatching {
                    downloadManager.getUriForDownloadedFile(record.managerId)?.toString()
                }.getOrNull() ?: snapshot.localUri
            } else {
                snapshot.localUri
            }
            val emptyFile = snapshot.status == DownloadStatus.SUCCEEDED &&
                snapshot.bytesDownloaded <= 0
            record.copy(
                status = if (emptyFile) DownloadStatus.FAILED else snapshot.status,
                statusMessage = if (emptyFile) strings.downloadMsgEmptyFile else snapshot.message,
                bytesDownloaded = snapshot.bytesDownloaded.coerceAtLeast(0),
                totalBytes = snapshot.totalBytes?.takeIf { it > 0 },
                localUri = uri,
                updatedAt = if (
                    record.status != snapshot.status ||
                    record.bytesDownloaded != snapshot.bytesDownloaded
                ) {
                    now
                } else {
                    record.updatedAt
                },
            )
        }
        }
    }

    override fun open(record: DownloadRecord): Boolean {
        val uri = contentUri(record) ?: return false
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, record.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return startSafely(intent)
    }

    override fun share(record: DownloadRecord): Boolean {
        val uri = contentUri(record) ?: return false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = record.mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(record.filename, uri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserTitle = String.format(strings.downloadShareChooserTitle, record.filename)
        return startSafely(Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun enqueueOne(
        manifest: MediaManifest,
        item: MediaItem,
        filename: String,
    ): DownloadRecord =
        if (item.type == MediaItemType.IMAGE) {
            cachedImageRecord(manifest, item, filename)
                ?: failedRecord(manifest, item, filename, strings.downloadStatusQueued).copy(
                    status = DownloadStatus.QUEUED,
                )
        } else {
            enqueueWithDownloadManager(manifest, item, filename)
        }

    private suspend fun downloadImage(record: DownloadRecord) {
        try {
            retryImageTransfer(
                onRetry = { number, waitMillis ->
                    waitForImageRetry(record.managerId, number, waitMillis)
                },
            ) {
                downloadImageOnce(record)
            }
        } catch (_: CancellationException) {
            // Cancellation and deletion have already updated the record.
        } catch (error: Exception) {
            replaceRecord(record.managerId) {
                if (it.isActive) it.copy(
                    status = DownloadStatus.FAILED,
                    statusMessage = downloadFailureMessage(error, strings),
                    updatedAt = System.currentTimeMillis(),
                ) else it
            }
        }
    }

    private suspend fun waitForImageRetry(managerId: Long, number: Int, waitMillis: Long): Boolean {
        val scheduled = synchronized(recordLock) {
            if (_records.value.none { it.managerId == managerId && it.isActive }) {
                false
            } else {
                replaceRecord(managerId) {
                    it.copy(
                        status = DownloadStatus.PAUSED,
                        statusMessage = String.format(strings.downloadRetrying, number, MAX_IMAGE_RETRIES),
                        bytesDownloaded = 0,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                true
            }
        }
        if (!scheduled) return false

        // Observe the record while waiting so cancelling or deleting it stops retries immediately.
        return awaitImageRetryWindow(records, managerId, waitMillis)
    }

    private suspend fun downloadImageOnce(record: DownloadRecord) {
        val resolver = appContext.contentResolver
        val imageCall = imageClient.newCall(
            Request.Builder().url(record.downloadUrl).apply {
                record.requestHeaders.forEach { (name, value) ->
                    if (name.isNotBlank() && value.isNotBlank()) header(name, value)
                }
            }.build(),
        )
        // Register and check the record under the same lock as cancellation.
        val registered = synchronized(recordLock) {
            if (_records.value.none { it.managerId == record.managerId && it.isActive }) {
                false
            } else {
                imageCalls[record.managerId] = imageCall
                replaceRecord(record.managerId) {
                    it.copy(
                        status = DownloadStatus.RUNNING,
                        statusMessage = strings.downloadStatusRunning,
                        bytesDownloaded = 0,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                true
            }
        }
        if (!registered) {
            imageCall.cancel()
            throw CancellationException("Download cancelled")
        }

        var destination: Uri? = null
        try {
            val jobContext = currentCoroutineContext()
            val bytes = imageCall.execute().use { response ->
                if (!response.isSuccessful) {
                    throw DownloadHttpException(
                        response.code,
                        parseRetryAfterMillis(response.header("Retry-After")),
                    )
                }
                destination = storageOperation {
                    resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, record.filename)
                            record.mimeType?.let { put(MediaStore.MediaColumns.MIME_TYPE, it) }
                            put(MediaStore.MediaColumns.RELATIVE_PATH,
                                "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_FOLDER")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        },
                    ) ?: error("Unable to create image destination")
                }
                val output = storageOperation {
                    resolver.openOutputStream(checkNotNull(destination), "w")
                        ?: error("Unable to open image destination")
                }
                storageOutputStream(output).use { output ->
                    var lastUpdate = 0L
                    copyImageResponse(response, output) { bytes, total ->
                        jobContext.ensureActive()
                        if (imageCall.isCanceled()) throw CancellationException()
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= 500) {
                            replaceRecord(record.managerId) {
                                if (it.isActive) it.copy(bytesDownloaded = bytes,
                                    totalBytes = total, updatedAt = now) else it
                            }
                            lastUpdate = now
                        }
                    }
                }
            }
            synchronized(recordLock) {
                if (_records.value.none { it.managerId == record.managerId && it.isActive }) {
                    throw CancellationException()
                }
                storageOperation {
                    check(resolver.update(checkNotNull(destination), ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }, null, null) > 0)
                }
                replaceRecord(record.managerId) {
                    it.copy(status = DownloadStatus.SUCCEEDED,
                        statusMessage = strings.downloadStatusSucceeded,
                        bytesDownloaded = bytes, totalBytes = bytes,
                        localUri = checkNotNull(destination).toString(),
                        updatedAt = System.currentTimeMillis())
                }
                destination = null
            }
        } finally {
            imageCalls.remove(record.managerId, imageCall)
            imageCall.cancel()
            destination?.let { uri -> runCatching { resolver.delete(uri, null, null) } }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun cachedImageRecord(
        manifest: MediaManifest,
        item: MediaItem,
        filename: String,
    ): DownloadRecord? {
        if (item.type != MediaItemType.IMAGE) return null
        val diskCache = appContext.imageLoader.diskCache ?: return null
        val snapshot = runCatching {
            diskCache.openSnapshot(item.downloadUrl)
        }.getOrNull() ?: return null

        return snapshot.use {
            val cachedFile = snapshot.data.toFile()
            val cachedBytes = cachedFile.length()
            if (cachedBytes <= 0L) return@use null

            val resolver = appContext.contentResolver
            var destination: Uri? = null
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    item.mimeType?.let { put(MediaStore.MediaColumns.MIME_TYPE, it) }
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_FOLDER",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                destination = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values,
                ) ?: error("Unable to create cached download destination")
                resolver.openOutputStream(checkNotNull(destination), "w")!!.use { output ->
                    cachedFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                resolver.update(
                    checkNotNull(destination),
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    },
                    null,
                    null,
                )

                val now = System.currentTimeMillis()
                DownloadRecord(
                    managerId = localId.getAndDecrement(),
                    platform = manifest.platform,
                    author = manifest.author,
                    sourceUrl = manifest.sourceUrl,
                    mediaId = item.id,
                    mediaType = item.type,
                    downloadUrl = item.downloadUrl,
                    thumbnailUrl = item.thumbnailUrl,
                    filename = filename,
                    mimeType = item.mimeType,
                    requestHeaders = item.requestHeaders,
                    status = DownloadStatus.SUCCEEDED,
                    statusMessage = strings.downloadStatusSucceeded,
                    bytesDownloaded = cachedBytes,
                    totalBytes = cachedBytes,
                    localUri = checkNotNull(destination).toString(),
                    createdAt = now,
                    updatedAt = now,
                )
            }.getOrElse {
                destination?.let { uri ->
                    runCatching { resolver.delete(uri, null, null) }
                }
                null
            }
        }
    }

    private fun enqueueWithDownloadManager(
        manifest: MediaManifest,
        item: MediaItem,
        filename: String,
    ): DownloadRecord {
        val now = System.currentTimeMillis()
        return try {
            val request = DownloadManager.Request(item.downloadUrl.toUri())
                .setTitle(filename)
                .setDescription(String.format(strings.downloadDescriptionFormat, manifest.platform.value))
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "$DOWNLOAD_FOLDER/$filename",
                )
            item.mimeType?.let(request::setMimeType)
            item.requestHeaders.forEach { (name, value) ->
                if (name.isNotBlank() && value.isNotBlank()) {
                    request.addRequestHeader(name, value)
                }
            }
            val id = downloadManager.enqueue(request)
            DownloadRecord(
                managerId = id,
                platform = manifest.platform,
                author = manifest.author,
                sourceUrl = manifest.sourceUrl,
                mediaId = item.id,
                mediaType = item.type,
                downloadUrl = item.downloadUrl,
                thumbnailUrl = item.thumbnailUrl,
                filename = filename,
                mimeType = item.mimeType,
                requestHeaders = item.requestHeaders,
                status = DownloadStatus.QUEUED,
                statusMessage = strings.downloadStatusQueued,
                bytesDownloaded = 0,
                totalBytes = item.contentLength,
                localUri = null,
                createdAt = now,
                updatedAt = now,
            )
        } catch (_: SecurityException) {
            failedRecord(manifest, item, filename, permissionFailureMessage(strings))
        } catch (_: IllegalStateException) {
            failedRecord(manifest, item, filename, strings.downloadStatusFailedFileErr)
        } catch (_: RuntimeException) {
            failedRecord(manifest, item, filename, strings.overlayErrorDefault)
        }
    }

    private fun failedRecord(
        manifest: MediaManifest,
        item: MediaItem,
        filename: String,
        message: String,
    ): DownloadRecord {
        val now = System.currentTimeMillis()
        return DownloadRecord(
            managerId = localId.getAndDecrement(),
            platform = manifest.platform,
            author = manifest.author,
            sourceUrl = manifest.sourceUrl,
            mediaId = item.id,
            mediaType = item.type,
            downloadUrl = item.downloadUrl,
            thumbnailUrl = item.thumbnailUrl,
            filename = filename,
            mimeType = item.mimeType,
            requestHeaders = item.requestHeaders,
            status = DownloadStatus.FAILED,
            statusMessage = message,
            bytesDownloaded = 0,
            totalBytes = item.contentLength,
            localUri = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun Cursor.toSnapshot(): DownloadSnapshot {
        val id = getLong(getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
        val rawStatus = getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val reason = getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        val bytes = getLong(getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val total = getLong(getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        val localUri = getStringOrNull(DownloadManager.COLUMN_LOCAL_URI)
        val status = when (rawStatus) {
            DownloadManager.STATUS_PENDING -> DownloadStatus.QUEUED
            DownloadManager.STATUS_RUNNING -> DownloadStatus.RUNNING
            DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCEEDED
            DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
            else -> DownloadStatus.FAILED
        }
        return DownloadSnapshot(
            managerId = id,
            status = status,
            message = downloadStatusMessage(status, reason),
            bytesDownloaded = bytes,
            totalBytes = total.takeIf { it > 0 },
            localUri = localUri,
        )
    }

    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun downloadStatusMessage(status: DownloadStatus, reason: Int): String = when (status) {
        DownloadStatus.QUEUED -> strings.downloadStatusQueued
        DownloadStatus.RUNNING -> strings.downloadStatusRunning
        DownloadStatus.PAUSED -> when (reason) {
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> strings.downloadStatusPausedNetwork
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> strings.downloadStatusPausedWifi
            DownloadManager.PAUSED_WAITING_TO_RETRY -> strings.downloadStatusPausedRetry
            else -> strings.downloadStatusPausedDefault
        }
        DownloadStatus.SUCCEEDED -> strings.downloadStatusSucceeded
        DownloadStatus.CANCELLED -> strings.downloadMsgCancelled
        DownloadStatus.FAILED -> when (reason) {
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> strings.downloadStatusFailedSpace
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> strings.downloadStatusFailedDuplicate
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> strings.downloadStatusFailedNoDevice
            DownloadManager.ERROR_FILE_ERROR -> strings.downloadStatusFailedFileErr
            DownloadManager.ERROR_HTTP_DATA_ERROR -> strings.errorNetwork
            DownloadManager.ERROR_CANNOT_RESUME -> strings.downloadFailureServer
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> strings.downloadFailureServer
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> strings.downloadFailureServer
            in 400..599 -> httpFailureMessage(reason, strings)
            else -> strings.overlayErrorDefault
        }
    }

    private fun replaceRecord(
        managerId: Long,
        transform: (DownloadRecord) -> DownloadRecord,
    ) {
        updateRecords { all -> all.map { if (it.managerId == managerId) transform(it) else it } }
    }

    private fun updateRecords(transform: (List<DownloadRecord>) -> List<DownloadRecord>) {
        synchronized(recordLock) {
            val sorted = transform(_records.value).sortedByDescending { it.createdAt }
            if (sorted == _records.value) return
            _records.value = sorted
            preferences.edit {
                putString(KEY_RECORDS, encodeRecords(sorted).toString())
            }
        }
    }

    private fun currentOccupiedFilenames(): Set<String> {
        val recorded = _records.value.map { it.filename }
        @Suppress("DEPRECATION")
        val files = runCatching {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                DOWNLOAD_FOLDER,
            ).list()?.toList().orEmpty()
        }.getOrDefault(emptyList())
        return (recorded + files).map { it.lowercase(Locale.US) }.toSet()
    }

    private fun uniqueFilename(desired: String, occupied: Set<String>): String {
        val dot = desired.lastIndexOf('.')
        val stem = if (dot > 0) desired.substring(0, dot) else desired
        val extension = if (dot > 0) desired.substring(dot) else ""
        var candidate = desired
        var suffix = 2
        while (candidate.lowercase(Locale.US) in occupied) {
            candidate = "${stem}_$suffix$extension"
            suffix += 1
        }
        return candidate
    }

    private fun contentUri(record: DownloadRecord): Uri? {
        if (record.status != DownloadStatus.SUCCEEDED) return null
        return runCatching {
            if (record.managerId > 0) {
                downloadManager.getUriForDownloadedFile(record.managerId)
            } else {
                record.localUri?.let(Uri::parse)
            }
        }.getOrNull()
    }

    private fun startSafely(intent: Intent): Boolean = runCatching {
        if (intent.resolveActivity(appContext.packageManager) == null) return false
        appContext.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun loadRecords(): List<DownloadRecord> {
        val raw = preferences.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                repeat(array.length()) { index ->
                    add(array.getJSONObject(index).toRecord())
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeRecords(records: List<DownloadRecord>): JSONArray = JSONArray().apply {
        records.forEach { put(it.toJson()) }
    }

    private fun DownloadRecord.toJson(): JSONObject = JSONObject().apply {
        put("managerId", managerId)
        put("platform", platform.name)
        put("author", author)
        put("sourceUrl", sourceUrl)
        put("mediaId", mediaId)
        put("mediaType", mediaType.name)
        put("downloadUrl", downloadUrl)
        put("thumbnailUrl", thumbnailUrl)
        put("filename", filename)
        put("mimeType", mimeType)
        put("headers", JSONObject(requestHeaders))
        put("status", status.name)
        put("statusMessage", statusMessage)
        put("bytesDownloaded", bytesDownloaded)
        put("totalBytes", totalBytes)
        put("localUri", localUri)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toRecord(): DownloadRecord {
        val headersObject = optJSONObject("headers") ?: JSONObject()
        val headers = buildMap {
            headersObject.keys().forEach { key -> put(key, headersObject.optString(key)) }
        }
        return DownloadRecord(
            managerId = getLong("managerId"),
            platform = enumValueOf(optString("platform", MediaPlatform.INSTAGRAM.name)),
            author = optNullableString("author"),
            sourceUrl = getString("sourceUrl"),
            mediaId = getString("mediaId"),
            mediaType = enumValueOf(optString("mediaType", MediaItemType.IMAGE.name)),
            downloadUrl = getString("downloadUrl"),
            thumbnailUrl = optNullableString("thumbnailUrl"),
            filename = getString("filename"),
            mimeType = optNullableString("mimeType"),
            requestHeaders = headers,
            status = enumValueOf(optString("status", DownloadStatus.FAILED.name)),
            statusMessage = optNullableString("statusMessage"),
            bytesDownloaded = optLong("bytesDownloaded", 0L),
            totalBytes = if (has("totalBytes") && !isNull("totalBytes")) getLong("totalBytes") else null,
            localUri = optNullableString("localUri"),
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private data class DownloadSnapshot(
        val managerId: Long,
        val status: DownloadStatus,
        val message: String?,
        val bytesDownloaded: Long,
        val totalBytes: Long?,
        val localUri: String?,
    )

    companion object {
        private const val PREFERENCES_NAME = "download_history"
        private const val KEY_RECORDS = "records"
        private const val DOWNLOAD_FOLDER = "IGThreads"
    }
}
