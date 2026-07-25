package com.jeremysu0818.igthreadsdl.domain.download

import com.jeremysu0818.igthreadsdl.domain.model.MediaItem
import com.jeremysu0818.igthreadsdl.domain.model.MediaManifest
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val records: StateFlow<List<DownloadRecord>>

    suspend fun enqueue(
        manifest: MediaManifest,
        items: List<MediaItem>,
    ): List<DownloadRecord>

    suspend fun cancel(managerId: Long)

    suspend fun retry(managerId: Long): DownloadRecord?

    suspend fun delete(managerId: Long): Boolean

    suspend fun refresh()

    fun open(record: DownloadRecord): Boolean

    fun share(record: DownloadRecord): Boolean
}
