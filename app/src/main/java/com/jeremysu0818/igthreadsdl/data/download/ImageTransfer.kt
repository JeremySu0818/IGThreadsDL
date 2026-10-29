package com.jeremysu0818.igthreadsdl.data.download

import okhttp3.Response
import java.io.OutputStream
import java.io.FilterOutputStream
import kotlinx.coroutines.CancellationException

internal fun copyImageResponse(
    response: Response,
    output: OutputStream,
    onProgress: (Long, Long?) -> Unit,
): Long {
    if (!response.isSuccessful) {
        throw DownloadHttpException(response.code, parseRetryAfterMillis(response.header("Retry-After")))
    }
    val body = response.body ?: throw DownloadContentException(empty = true)
    val total = body.contentLength().takeIf { it > 0 }
    var bytes = 0L
    body.byteStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            output.write(buffer, 0, count)
            bytes += count
            onProgress(bytes, total)
        }
    }
    if (bytes == 0L || (total != null && bytes != total)) {
        throw DownloadContentException(empty = bytes == 0L)
    }
    return bytes
}

internal inline fun <T> storageOperation(action: () -> T): T =
    try {
        action()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw DownloadStorageException(error)
    }


internal fun storageOutputStream(output: OutputStream): OutputStream =
    object : FilterOutputStream(output) {
        override fun write(b: Int) = storageOperation { out.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) =
            storageOperation { out.write(b, off, len) }
        override fun flush() = storageOperation { out.flush() }
        override fun close() = storageOperation { out.close() }
    }
