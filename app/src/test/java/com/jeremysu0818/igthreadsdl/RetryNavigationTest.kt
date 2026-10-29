package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord
import com.jeremysu0818.igthreadsdl.domain.download.DownloadStatus
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.model.MediaPlatform
import com.jeremysu0818.igthreadsdl.ui.MainTab
import com.jeremysu0818.igthreadsdl.ui.RetryNavigation
import org.junit.Assert.*
import org.junit.Test

class RetryNavigationTest {
    @Test fun terminalRetryReturnsToOriginalRecord() {
        val navigation = RetryNavigation()
        val token = navigation.begin()
        val original = record(1, DownloadStatus.FAILED)
        for (status in listOf(DownloadStatus.SUCCEEDED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)) {
            val retry = record(2, status)
            assertEquals(1L, navigation.historyTargetId(token, MainTab.QUEUE, 1, 2, listOf(retry, original)))
        }
    }

    @Test fun activeOrRemovedRetryDoesNotNavigate() {
        val navigation = RetryNavigation()
        val token = navigation.begin()
        for (status in listOf(DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PAUSED)) {
            assertNull(navigation.historyTargetId(token, MainTab.QUEUE, 1, 2, listOf(record(2, status))))
        }
        assertNull(navigation.historyTargetId(token, MainTab.QUEUE, 1, 2, emptyList()))
    }

    @Test fun removedOriginalFallsBackToNewRecord() {
        val navigation = RetryNavigation()
        val token = navigation.begin()
        assertEquals(2L, navigation.historyTargetId(token, MainTab.QUEUE, 1, 2,
            listOf(record(2, DownloadStatus.SUCCEEDED))))
    }

    @Test fun latestRetryWinsAndManualNavigationIsRespected() {
        val navigation = RetryNavigation()
        val first = navigation.begin()
        val second = navigation.begin()
        val records = listOf(record(2, DownloadStatus.SUCCEEDED), record(1, DownloadStatus.FAILED))
        assertNull(navigation.historyTargetId(first, MainTab.QUEUE, 1, 2, records))
        assertNull(navigation.historyTargetId(second, MainTab.SETTINGS, 1, 2, records))
        navigation.leaveQueue()
        assertNull(navigation.historyTargetId(second, MainTab.QUEUE, 1, 2, records))
    }

    private fun record(id: Long, status: DownloadStatus) = DownloadRecord(
        managerId = id, platform = MediaPlatform.THREADS, author = null,
        sourceUrl = "https://www.threads.net/@test/post/123", mediaId = "$id",
        mediaType = MediaItemType.IMAGE, downloadUrl = "https://example.com/image.jpg",
        thumbnailUrl = null, filename = "image.jpg", mimeType = "image/jpeg",
        requestHeaders = emptyMap(), status = status, statusMessage = null,
        bytesDownloaded = 0, totalBytes = null, localUri = null,
        createdAt = 0, updatedAt = 0,
    )
}
