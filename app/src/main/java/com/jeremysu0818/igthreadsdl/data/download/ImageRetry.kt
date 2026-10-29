package com.jeremysu0818.igthreadsdl.data.download

import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.random.Random

internal const val MAX_IMAGE_RETRIES = 3
private const val MAX_SERVER_WAIT_MS = 60_000L
private val RETRY_DELAYS_MS = longArrayOf(2_000, 4_000, 8_000)
private val RATE_LIMIT_DELAYS_MS = longArrayOf(15_000, 30_000, 60_000)

internal fun imageRetryDelayMillis(error: Exception, retryNumber: Int): Long? {
    if (retryNumber !in 1..MAX_IMAGE_RETRIES) return null
    val retryable = when (error) {
        is CancellationException, is DownloadStorageException,
        is SSLHandshakeException, is SSLPeerUnverifiedException -> false
        is DownloadHttpException -> error.code == 408 || error.code == 429 ||
            error.code in setOf(500, 502, 503, 504)
        is IOException -> true
        else -> false
    }
    if (!retryable) return null

    val retryAfter = (error as? DownloadHttpException)?.retryAfterMillis ?: 0L
    if (retryAfter > MAX_SERVER_WAIT_MS) return null
    val delays = if ((error as? DownloadHttpException)?.code == 429) {
        RATE_LIMIT_DELAYS_MS
    } else {
        RETRY_DELAYS_MS
    }
    val base = maxOf(delays[retryNumber - 1], retryAfter)
    // Spread simultaneous failures across a batch while waiting at least as long as requested.
    return base + Random.nextLong(minOf(base / 4, 2_000L) + 1)
}

internal fun parseRetryAfterMillis(value: String?, nowMillis: Long = System.currentTimeMillis()): Long? {
    val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (text.all(Char::isDigit)) {
        val seconds = text.toLongOrNull() ?: return Long.MAX_VALUE
        return if (seconds > Long.MAX_VALUE / 1_000) Long.MAX_VALUE
        else TimeUnit.SECONDS.toMillis(seconds)
    }
    return runCatching {
        val deadline = ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant().toEpochMilli()
        (deadline - nowMillis).coerceAtLeast(0L)
    }.getOrNull()
}

internal suspend fun awaitImageRetryWindow(
    records: StateFlow<List<DownloadRecord>>,
    managerId: Long,
    waitMillis: Long,
): Boolean = withTimeoutOrNull(waitMillis) {
    records.first { all -> all.none { it.managerId == managerId && it.isActive } }
    false
} ?: records.value.any { it.managerId == managerId && it.isActive }

/** Calls onRetry after an attempt fails; returning false stops without another attempt. */
internal suspend fun <T> retryImageTransfer(
    onRetry: suspend (number: Int, waitMillis: Long) -> Boolean,
    attempt: suspend () -> T,
): T {
    var retryCount = 0
    while (true) {
        try {
            return attempt()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val nextRetry = retryCount + 1
            val waitMillis = imageRetryDelayMillis(error, nextRetry) ?: throw error
            retryCount = nextRetry
            if (!onRetry(nextRetry, waitMillis)) throw CancellationException("Download cancelled")
        }
    }
}
