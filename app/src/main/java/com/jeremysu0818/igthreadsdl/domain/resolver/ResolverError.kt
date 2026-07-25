package com.jeremysu0818.igthreadsdl.domain.resolver

import com.jeremysu0818.igthreadsdl.i18n.AppStrings

sealed class ResolverError(open val detail: String? = null) {
    data class InvalidUrl(override val detail: String? = null) : ResolverError(detail)

    data class UnsupportedPlatform(override val detail: String? = null) : ResolverError(detail)

    data class PrivateContent(override val detail: String? = null) : ResolverError(detail)

    data class LoginRequired(override val detail: String? = null) : ResolverError(detail)

    data class ContentNotFound(override val detail: String? = null) : ResolverError(detail)

    data class RateLimited(override val detail: String? = null) : ResolverError(detail)

    data class HtmlStructureChanged(
        val platform: String,
        override val detail: String? = null,
    ) : ResolverError(detail)

    data class Network(override val detail: String? = null) : ResolverError(detail)

    data class Http(
        val statusCode: Int,
        override val detail: String? = null,
    ) : ResolverError(detail)

    data class EmptyMedia(override val detail: String? = null) : ResolverError(detail)

    fun userMessage(strings: AppStrings): String = when (this) {
        is InvalidUrl -> strings.errorInvalidUrl
        is UnsupportedPlatform -> strings.errorUnsupportedPlatform
        is PrivateContent -> strings.errorPrivateContent
        is LoginRequired -> strings.errorLoginRequired
        is ContentNotFound -> strings.errorContentNotFound
        is RateLimited -> strings.errorRateLimited
        is HtmlStructureChanged -> {
            if (platform == "threads") {
                strings.errorHtmlChangedThreads
            } else {
                strings.errorHtmlChangedInstagram
            }
        }
        is Network -> strings.errorNetwork
        is Http -> String.format(strings.errorHttp, statusCode)
        is EmptyMedia -> strings.errorEmptyMedia
    }

    fun userMessage(): String = userMessage(
        com.jeremysu0818.igthreadsdl.i18n.LanguageManager.getStrings(
            com.jeremysu0818.igthreadsdl.i18n.LanguageManager.getSavedLanguage(
                com.jeremysu0818.igthreadsdl.AppGraph.application
            )
        )
    )

    companion object {
        fun fromHttpStatus(
            statusCode: Int,
            responseText: String = "",
            platform: String,
        ): ResolverError {
            val body = responseText.lowercase()
            return when {
                statusCode == 404 || statusCode == 410 -> ContentNotFound()
                statusCode == 429 -> RateLimited()
                statusCode == 401 -> LoginRequired()
                statusCode == 403 && ("private" in body || "not authorized" in body) -> PrivateContent()
                statusCode == 403 -> LoginRequired()
                statusCode in 500..599 -> Http(statusCode, "$platform resolver server error")
                else -> Http(statusCode)
            }
        }
    }
}
