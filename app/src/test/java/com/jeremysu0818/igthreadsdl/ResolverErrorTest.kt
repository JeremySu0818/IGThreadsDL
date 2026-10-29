package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverError
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolverErrorTest {
    @Test
    fun ambiguousForbiddenIsNotReportedAsLoginAndRateLimitTakesPriority() {
        assertTrue(ResolverError.fromHttpStatus(403, "", "instagram") is ResolverError.PrivateContent)
        assertTrue(ResolverError.fromHttpStatus(429, """{"message":"login_required"}""", "threads")
            is ResolverError.RateLimited)
        assertTrue(ResolverError.fromHttpStatus(503, """{"message":"login_required"}""", "instagram")
            is ResolverError.Http)
    }

    @Test
    fun structuredLoginErrorsKeepPlatformAndIgnoreUnrelatedScriptText() {
        val error = ResolverError.fromHttpStatus(400, """{"message": "challenge_required"}""", "threads")
        assertTrue(error is ResolverError.LoginRequired)
        org.junit.Assert.assertEquals("threads", (error as ResolverError.LoginRequired).platform)
        org.junit.Assert.assertNull(ResolverError.authenticationError(
            """{"login_required":false,"translation":"login_required"}""", "instagram",
        ))
    }

    @Test
    fun localizedMessagesKeepPlatformsSeparateAndDoNotExposeTechnicalDetails() {
        val file = java.io.File("src/main/assets/strings.json").takeIf { it.exists() }
            ?: java.io.File("app/src/main/assets/strings.json")
        file.inputStream().use(com.jeremysu0818.igthreadsdl.i18n.LanguageManager::loadTranslations)
        for (language in com.jeremysu0818.igthreadsdl.i18n.AppLanguage.supportedLanguages) {
            val strings = com.jeremysu0818.igthreadsdl.i18n.LanguageManager.getStrings(language)
            val threads = ResolverError.LoginRequired(platform = "threads").userMessage(strings)
            assertTrue(threads.contains("Threads"))
            org.junit.Assert.assertFalse(threads.contains("Instagram"))
            org.junit.Assert.assertEquals(strings.errorLoginRequired, ResolverError.LoginRequired().userMessage(strings))
            org.junit.Assert.assertEquals(strings.errorMissingVideo, ResolverError.MissingVideo("secret-token").userMessage(strings))
            org.junit.Assert.assertEquals(strings.errorShareLinkUnresolved, ResolverError.ShareLinkUnresolved().userMessage(strings))
            org.junit.Assert.assertFalse(ResolverError.HtmlStructureChanged("threads", "secret-token")
                .userMessage(strings).contains("secret-token"))
        }
        val zh = com.jeremysu0818.igthreadsdl.i18n.LanguageManager
            .getStrings(com.jeremysu0818.igthreadsdl.i18n.AppLanguage.ZH_TW)
        assertTrue(zh.errorHtmlChangedInstagram.contains("可能"))
        assertTrue(zh.errorHtmlChangedThreads.contains("可能"))
        org.junit.Assert.assertFalse(zh.errorContentNotFound.contains("24"))
    }

    @Test
    fun httpStatusMapsToSpecificErrors() {
        assertTrue(
            ResolverError.fromHttpStatus(404, "", "instagram") is ResolverError.ContentNotFound,
        )
        assertTrue(
            ResolverError.fromHttpStatus(429, "", "threads") is ResolverError.RateLimited,
        )
        assertTrue(
            ResolverError.fromHttpStatus(403, "private media", "instagram") is
                ResolverError.PrivateContent,
        )
        assertTrue(
            ResolverError.fromHttpStatus(401, "", "instagram") is ResolverError.LoginRequired,
        )
    }
}
