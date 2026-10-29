package com.jeremysu0818.igthreadsdl.data.download

import com.jeremysu0818.igthreadsdl.i18n.AppStrings
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal class DownloadHttpException(val code: Int, val retryAfterMillis: Long? = null) : IOException()
internal class DownloadContentException(val empty: Boolean) : IOException()
internal class DownloadStorageException(cause: Throwable) : IOException(cause)

internal fun httpFailureMessage(code: Int, strings: AppStrings): String = when (code) {
    401, 403, 404, 410 -> strings.downloadFailureUnavailable
    429 -> strings.errorRateLimited
    else -> strings.downloadFailureServer
}

internal fun downloadFailureMessage(error: Throwable, strings: AppStrings): String =
    when (error) {
        is DownloadHttpException -> httpFailureMessage(error.code, strings)
        is DownloadContentException ->
            if (error.empty) strings.downloadMsgEmptyFile else strings.errorNetwork
        is DownloadStorageException ->
            if (error.cause is SecurityException) permissionFailureMessage(strings)
            else strings.downloadStatusFailedFileErr
        is SecurityException -> permissionFailureMessage(strings)
        is UnknownHostException, is SocketException, is InterruptedIOException, is SSLException,
        is IOException -> strings.errorNetwork
        else -> strings.overlayErrorDefault
    }

internal fun permissionFailureMessage(strings: AppStrings): String =
    strings.downloadErrPermission.substringBefore("%1\$s").trimEnd(' ', ':', '：')

/**
 * Older versions persisted exception messages in these translated templates. Replace those
 * details for every supported language while keeping previously saved, user-facing messages.
 */
internal fun cleanLegacyDownloadFailure(
    message: String,
    translations: Iterable<AppStrings>,
    strings: AppStrings,
): String {
    for (translation in translations) {
        val codeTemplate = translation.downloadStatusFailedCode
        val codePrefix = codeTemplate.substringBefore("%1\$d")
        if (codePrefix.isNotBlank() && codePrefix != codeTemplate && message.startsWith(codePrefix)) {
            val code = Regex("""^\s*(\d+)""").find(message.removePrefix(codePrefix))
                ?.groupValues?.get(1)?.toIntOrNull()
            return if (code != null && code in 400..599) httpFailureMessage(code, strings)
            else strings.overlayErrorDefault
        }
        val templates = listOf(
            translation.downloadErrCannotCreate to 0,
            translation.downloadErrPermission to 1,
            translation.downloadErrStorage to 2,
        )
        for ((template, kind) in templates) {
            val prefix = template.substringBefore("%1\$s")
            if (prefix.isBlank() || prefix == template || !message.startsWith(prefix)) continue
            val details = message.removePrefix(prefix)
            val httpCode = Regex("""(?i)\bHTTP\s+(\d{3})\b""")
                .find(details)?.groupValues?.get(1)?.toIntOrNull()
            if (httpCode != null) return httpFailureMessage(httpCode, strings)
            if (listOf(
                    "resolve host", "unknownhost", "timed out", "timeout",
                    "failed to connect", "connection refused", "connection reset",
                    "network is unreachable", "no route to host", "unable to connect",
                ).any { details.contains(it, ignoreCase = true) }
            ) return strings.errorNetwork
            return when (kind) {
                1 -> permissionFailureMessage(strings)
                2 -> strings.downloadStatusFailedFileErr
                else -> strings.overlayErrorDefault
            }
        }
    }
    return message
}
