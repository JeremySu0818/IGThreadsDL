package com.jeremysu0818.igthreadsdl.data.session

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object InstagramSessionManager {
    const val INSTAGRAM_BASE_URL = "https://www.instagram.com/"

    fun cookieHeader(): String? = readCookies()
        ?.takeIf { cookies ->
            !parseCookies(cookies)[SESSION_COOKIE_NAME].isNullOrBlank()
        }

    fun isLoggedIn(): Boolean = cookieHeader() != null

    fun csrfToken(cookieHeader: String): String? =
        parseCookies(cookieHeader)[CSRF_COOKIE_NAME]?.takeIf { it.isNotBlank() }

    fun flush() {
        runOnMain {
            CookieManager.getInstance().flush()
        }
    }

    fun clearSession(onComplete: (() -> Unit)? = null) {
        val clear = {
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                onComplete?.invoke()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            clear()
        } else {
            Handler(Looper.getMainLooper()).post(clear)
        }
    }

    internal fun parseCookies(header: String): Map<String, String> =
        header.split(';')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val name = part.substring(0, separator).trim()
                    .takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                name to
                    part.substring(separator + 1).trim()
            }
            .toMap()

    private fun readCookies(): String? = runOnMain {
        CookieManager.getInstance()
            .getCookie(INSTAGRAM_BASE_URL)
            ?.takeIf { it.isNotBlank() }
    }

    private fun <T> runOnMain(block: () -> T): T? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return runCatching(block).getOrNull()
        }
        val latch = CountDownLatch(1)
        var result: T? = null
        Handler(Looper.getMainLooper()).post {
            result = runCatching(block).getOrNull()
            latch.countDown()
        }
        latch.await(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        return result
    }

    private const val SESSION_COOKIE_NAME = "sessionid"
    private const val CSRF_COOKIE_NAME = "csrftoken"
    private const val MAIN_THREAD_TIMEOUT_SECONDS = 3L
}
