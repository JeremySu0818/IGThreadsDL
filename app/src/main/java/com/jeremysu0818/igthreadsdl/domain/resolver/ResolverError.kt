package com.jeremysu0818.igthreadsdl.domain.resolver

import com.jeremysu0818.igthreadsdl.i18n.AppStrings

sealed class ResolverError(open val detail: String? = null) {
    data class InvalidUrl(override val detail: String? = null) : ResolverError(detail)

    data class UnsupportedPlatform(override val detail: String? = null) : ResolverError(detail)

    data class PrivateContent(override val detail: String? = null) : ResolverError(detail)

    data class LoginRequired(
        override val detail: String? = null,
        val platform: String = "instagram",
    ) : ResolverError(detail)

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

    data class MissingVideo(override val detail: String? = null) : ResolverError(detail)

    data class ShareLinkUnresolved(override val detail: String? = null) : ResolverError(detail)

    fun userMessage(strings: AppStrings): String = when (this) {
        is InvalidUrl -> strings.errorInvalidUrl
        is UnsupportedPlatform -> strings.errorUnsupportedPlatform
        is PrivateContent -> strings.errorPrivateContent
        is LoginRequired -> if (platform == "threads") strings.errorLoginRequiredThreads else strings.errorLoginRequired
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
        is MissingVideo -> strings.errorMissingVideo
        is ShareLinkUnresolved -> strings.errorShareLinkUnresolved
    }

    fun userMessage(): String = userMessage(
        com.jeremysu0818.igthreadsdl.i18n.LanguageManager.getStrings(
            com.jeremysu0818.igthreadsdl.i18n.LanguageManager.getSavedLanguage(
                com.jeremysu0818.igthreadsdl.AppGraph.application
            )
        )
    )

    companion object {
        fun authenticationError(responseText: String, platform: String): LoginRequired? {
            val json = runCatching { org.json.JSONObject(responseText) }.getOrNull() ?: return null
            val message = json.optString("message").lowercase(java.util.Locale.ROOT)
            return if (json.optBoolean("login_required") ||
                message in setOf("login_required", "login required", "checkpoint_required", "challenge_required")
            ) LoginRequired(platform = platform) else null
        }

        fun fromHttpStatus(
            statusCode: Int,
            responseText: String = "",
            platform: String,
        ): ResolverError {
            return when {
                statusCode == 404 || statusCode == 410 -> ContentNotFound()
                statusCode == 429 -> RateLimited()
                statusCode == 401 -> LoginRequired(platform = platform)
                statusCode in 500..599 -> Http(statusCode, "$platform resolver server error")
                authenticationError(responseText, platform) != null -> authenticationError(responseText, platform)!!
                statusCode == 403 -> PrivateContent()
                else -> Http(statusCode)
            }
        }
    }
}
