package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.download.DownloadContentException
import com.jeremysu0818.igthreadsdl.data.download.DownloadHttpException
import com.jeremysu0818.igthreadsdl.data.download.DownloadStorageException
import com.jeremysu0818.igthreadsdl.data.download.MAX_IMAGE_RETRIES
import com.jeremysu0818.igthreadsdl.data.download.awaitImageRetryWindow
import com.jeremysu0818.igthreadsdl.data.download.copyImageResponse
import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord
import com.jeremysu0818.igthreadsdl.domain.download.DownloadStatus
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.model.MediaPlatform
import com.jeremysu0818.igthreadsdl.data.download.imageRetryDelayMillis
import com.jeremysu0818.igthreadsdl.data.download.parseRetryAfterMillis
import com.jeremysu0818.igthreadsdl.data.download.retryImageTransfer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLHandshakeException

class ImageRetryTest {
    @Test fun transientHttpFailuresThenSuccessUseFreshRequests() = runBlocking {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            val code = if (calls <= 2) 503 else 200
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body((if (code == 200) "image bytes" else "unavailable").toResponseBody())
                .build()
        }.build()
        val request = Request.Builder().url("https://example.invalid/image.jpg").build()
        val scheduled = mutableListOf<Int>()
        val image = retryImageTransfer(
            onRetry = { number, _ -> scheduled += number; true },
        ) {
            client.newCall(request).execute().use { response ->
                val output = ByteArrayOutputStream()
                copyImageResponse(response, output) { _, _ -> }
                output.toString("UTF-8")
            }
        }

        assertEquals("image bytes", image)
        assertEquals(3, calls)
        assertEquals(listOf(1, 2), scheduled)
    }

    @Test fun persistentNetworkFailureStopsAfterThreeRetries() {
        var attempts = 0
        val waits = mutableListOf<Long>()
        assertThrows(UnknownHostException::class.java) {
            runBlocking {
                retryImageTransfer(
                    onRetry = { number, wait ->
                        assertEquals(waits.size + 1, number)
                        waits += wait
                        true
                    },
                ) {
                    attempts++
                    throw UnknownHostException("offline")
                }
            }
        }
        assertEquals(MAX_IMAGE_RETRIES + 1, attempts)
        assertEquals(3, waits.size)
        assertTrue(waits[0] in 2_000L..2_500L)
        assertTrue(waits[1] in 4_000L..5_000L)
        assertTrue(waits[2] in 8_000L..10_000L)
    }

    @Test fun onlyTemporaryFailuresAreRetried() {
        val transient = listOf(
            UnknownHostException(), SocketTimeoutException(),
            ProtocolException(), DownloadContentException(false),
            DownloadHttpException(408), DownloadHttpException(429),
            DownloadHttpException(500), DownloadHttpException(502),
            DownloadHttpException(503), DownloadHttpException(504),
        )
        transient.forEach { assertNotNull(imageRetryDelayMillis(it, 1)) }

        val permanent = listOf(
            DownloadHttpException(401), DownloadHttpException(403),
            DownloadHttpException(404), DownloadHttpException(410),
            DownloadHttpException(501), DownloadStorageException(IOException()),
            SecurityException(), SSLHandshakeException("invalid certificate"),
            IllegalArgumentException(),
        )
        permanent.forEach { assertNull(imageRetryDelayMillis(it, 1)) }
        assertNull(imageRetryDelayMillis(UnknownHostException(), 4))
    }

    @Test fun serverRetryAfterIsHonoredWithoutKeepingQueueIndefinitely() {
        val now = Instant.parse("2026-10-29T12:00:00Z").toEpochMilli()
        assertEquals(45_000L, parseRetryAfterMillis("45", now))
        assertEquals(10_000L, parseRetryAfterMillis("Thu, 29 Oct 2026 12:00:10 GMT", now))
        assertEquals(0L, parseRetryAfterMillis("Thu, 29 Oct 2026 11:59:50 GMT", now))
        assertNull(parseRetryAfterMillis("invalid", now))
        assertEquals(Long.MAX_VALUE, parseRetryAfterMillis("9999999999999999999999", now))

        val requestedDelay = imageRetryDelayMillis(DownloadHttpException(429, 45_000), 1)
        assertNotNull(requestedDelay)
        assertTrue(requestedDelay!! in 45_000L..47_000L)
        assertTrue(imageRetryDelayMillis(DownloadHttpException(429), 1)!! in 15_000L..17_000L)
        assertNull(imageRetryDelayMillis(DownloadHttpException(429, 61_000), 1))
        assertNull(imageRetryDelayMillis(DownloadHttpException(503, 61_000), 1))
    }

    @Test fun stoppingWhileWaitingDoesNotStartAnotherAttempt() {
        var attempts = 0
        assertThrows(CancellationException::class.java) {
            runBlocking {
                retryImageTransfer(
                    onRetry = { _, _ -> false },
                ) {
                    attempts++
                    throw UnknownHostException()
                }
            }
        }
        assertEquals(1, attempts)
    }

    @Test fun cancelledOrDeletedRecordInterruptsRetryWindow() = runBlocking {
        val terminalStates = listOf(
            listOf(record(7, DownloadStatus.CANCELLED)),
            emptyList(),
        )
        terminalStates.forEach { terminal ->
            val records = MutableStateFlow(listOf(record(7, DownloadStatus.PAUSED)))
            val waiting = async(start = CoroutineStart.UNDISPATCHED) {
                awaitImageRetryWindow(records, 7, 30_000)
            }
            records.value = terminal
            assertFalse(withTimeout(1_000) { waiting.await() })
        }
        val active = MutableStateFlow(listOf(record(7, DownloadStatus.PAUSED)))
        assertTrue(withTimeout(1_000) { awaitImageRetryWindow(active, 7, 10) })
    }

    @Test fun cancellingCoroutineDuringBackoffStopsImmediately() = runBlocking {
        var attempts = 0
        val waiting = CompletableDeferred<Unit>()
        val job = launch {
            retryImageTransfer(
                onRetry = { _, _ ->
                    waiting.complete(Unit)
                    awaitCancellation()
                },
            ) {
                attempts++
                throw UnknownHostException()
            }
        }
        waiting.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertEquals(1, attempts)
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
