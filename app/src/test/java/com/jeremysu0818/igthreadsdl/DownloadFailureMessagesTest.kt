package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.download.DownloadContentException
import com.jeremysu0818.igthreadsdl.data.download.DownloadHttpException
import com.jeremysu0818.igthreadsdl.data.download.DownloadStorageException
import com.jeremysu0818.igthreadsdl.data.download.cleanLegacyDownloadFailure
import com.jeremysu0818.igthreadsdl.data.download.downloadFailureMessage
import com.jeremysu0818.igthreadsdl.data.download.permissionFailureMessage
import com.jeremysu0818.igthreadsdl.data.download.storageOutputStream
import com.jeremysu0818.igthreadsdl.i18n.AppLanguage
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class DownloadFailureMessagesTest {
    @Before fun loadCatalog() {
        val file = File("src/main/assets/strings.json").takeIf { it.exists() }
            ?: File("app/src/main/assets/strings.json")
        file.inputStream().use(LanguageManager::loadTranslations)
    }

    @Test fun networkErrorsNeverExposeExceptionDetails() {
        val strings = LanguageManager.getStrings(AppLanguage.ZH_TW)
        val failures = listOf(
            UnknownHostException("secret.example.com"),
            SocketTimeoutException("read timed out"),
            SocketException("connection reset"),
            SSLException("handshake failed"),
            IOException("unexpected network failure"),
        )
        failures.forEach { error ->
            assertEquals(strings.errorNetwork, downloadFailureMessage(error, strings))
        }
    }

    @Test fun httpFailuresHaveUsefulLocalizedMessages() {
        val strings = LanguageManager.getStrings(AppLanguage.ZH_TW)
        listOf(401, 403, 404, 410).forEach {
            assertEquals(strings.downloadFailureUnavailable, downloadFailureMessage(DownloadHttpException(it), strings))
        }
        assertEquals(strings.errorRateLimited, downloadFailureMessage(DownloadHttpException(429), strings))
        assertEquals(strings.downloadFailureServer, downloadFailureMessage(DownloadHttpException(503), strings))
    }

    @Test fun storageFailuresRemainDistinctFromNetworkFailures() {
        val strings = LanguageManager.getStrings(AppLanguage.ZH_TW)
        val output = storageOutputStream(object : OutputStream() {
            override fun write(b: Int) { throw IOException("/private/location") }
        })
        val error = assertThrows(DownloadStorageException::class.java) { output.write(1) }
        assertEquals(strings.downloadStatusFailedFileErr, downloadFailureMessage(error, strings))
        assertEquals(permissionFailureMessage(strings),
            downloadFailureMessage(DownloadStorageException(SecurityException("private path")), strings))
        assertEquals(strings.downloadMsgEmptyFile, downloadFailureMessage(DownloadContentException(true), strings))
        assertEquals(strings.errorNetwork, downloadFailureMessage(DownloadContentException(false), strings))
        assertEquals(strings.overlayErrorDefault, downloadFailureMessage(IllegalArgumentException("secret"), strings))
    }

    @Test fun legacyExceptionsFromEveryLanguageAreCleaned() {
        val current = LanguageManager.getStrings(AppLanguage.ZH_TW)
        val translations = AppLanguage.supportedLanguages.map { LanguageManager.getStrings(it) }
        translations.forEach { original ->
            val message = String.format(original.downloadErrCannotCreate,
                "Unable to resolve host \"secret.example.com\": No address associated with hostname")
            assertEquals(current.errorNetwork, cleanLegacyDownloadFailure(message, translations, current))
        }
        val english = LanguageManager.getStrings(AppLanguage.EN)
        assertEquals(current.downloadFailureUnavailable, cleanLegacyDownloadFailure(
            String.format(english.downloadErrCannotCreate, "HTTP 403"), translations, current))
        assertEquals(current.overlayErrorDefault, cleanLegacyDownloadFailure(
            String.format(english.downloadErrCannotCreate, "unexpected internal detail"), translations, current))
        assertEquals(current.downloadStatusFailedFileErr, cleanLegacyDownloadFailure(
            String.format(english.downloadErrStorage, "/private/path"), translations, current))
        assertEquals(current.downloadFailureServer, cleanLegacyDownloadFailure(
            String.format(english.downloadStatusFailedCode, 503), translations, current))
        assertEquals(current.overlayErrorDefault, cleanLegacyDownloadFailure(
            String.format(english.downloadStatusFailedCode, 1010), translations, current))
        assertEquals(current.downloadHlsNotSupported,
            cleanLegacyDownloadFailure(current.downloadHlsNotSupported, translations, current))
    }
}
