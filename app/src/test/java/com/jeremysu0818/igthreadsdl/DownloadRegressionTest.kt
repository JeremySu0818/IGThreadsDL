package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.download.copyImageResponse
import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord
import com.jeremysu0818.igthreadsdl.domain.download.DownloadStatus
import com.jeremysu0818.igthreadsdl.domain.download.allSucceeded
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.model.MediaPlatform
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException

class DownloadRegressionTest {
    @Test fun cancelledBatchDoesNotCompleteSelection() {
        assertFalse(listOf(record(1, DownloadStatus.CANCELLED)).allSucceeded(setOf(1)))
        assertFalse(listOf(record(1, DownloadStatus.SUCCEEDED),
            record(2, DownloadStatus.CANCELLED)).allSucceeded(setOf(1, 2)))
    }

    @Test fun onlyEveryRequestedSuccessCompletesSelection() {
        assertTrue(listOf(record(1, DownloadStatus.SUCCEEDED),
            record(2, DownloadStatus.SUCCEEDED)).allSucceeded(setOf(1, 2)))
        assertFalse(listOf(record(1, DownloadStatus.SUCCEEDED)).allSucceeded(setOf(1, 2)))
        assertFalse(emptyList<DownloadRecord>().allSucceeded(emptySet()))
        DownloadStatus.entries.filter { it != DownloadStatus.SUCCEEDED }.forEach { status ->
            assertFalse(listOf(record(1, status)).allSucceeded(setOf(1)))
        }
    }

    @Test fun imageDownloadsWithoutPreviewCache() {
        val output = ByteArrayOutputStream()
        response("image bytes").use { response ->
            assertEquals(11L, copyImageResponse(response, output) { _, _ -> })
        }
        assertEquals("image bytes", output.toString("UTF-8"))
    }

    @Test fun emptyOrHttpErrorResponseCannotSucceed() {
        listOf(response(""), response("error", 503)).forEach { response ->
            response.use {
                assertThrows(IOException::class.java) {
                    copyImageResponse(it, ByteArrayOutputStream()) { _, _ -> }
                }
            }
        }
    }

    @Test fun truncatedImageCannotSucceed() {
        val truncatedBody = object : okhttp3.ResponseBody() {
            override fun contentType(): okhttp3.MediaType? = null
            override fun contentLength() = 100L
            override fun source(): okio.BufferedSource = okio.Buffer().writeUtf8("partial")
        }
        response("unused").newBuilder().body(truncatedBody).build().use {
            assertThrows(IOException::class.java) {
                copyImageResponse(it, ByteArrayOutputStream()) { _, _ -> }
            }
        }
    }

    @Test fun cancellationInterruptsImageCopy() {
        response("image bytes").use {
            assertThrows(CancellationException::class.java) {
                copyImageResponse(it, ByteArrayOutputStream()) { _, _ ->
                    throw CancellationException()
                }
            }
        }
    }

    private fun response(body: String, code: Int = 200) = Response.Builder()
        .request(Request.Builder().url("https://example.com/image.jpg").build())
        .protocol(Protocol.HTTP_1_1).code(code).message("test")
        .body(body.toResponseBody()).build()

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
