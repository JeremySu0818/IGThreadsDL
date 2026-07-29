package com.jeremysu0818.igthreadsdl.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jeremysu0818.igthreadsdl.data.session.InstagramSessionManager
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager
import com.jeremysu0818.igthreadsdl.i18n.AppLanguage
import com.jeremysu0818.igthreadsdl.ui.theme.IGThreadsDLTheme
import com.jeremysu0818.igthreadsdl.ui.theme.ThemeMode

class InstagramLoginActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var completed = false
    private var statusText by mutableStateOf("")

    private val sessionPoll = object : Runnable {
        override fun run() {
            if (completed || isFinishing || isDestroyed) return
            if (InstagramSessionManager.isLoggedIn()) {
                completeLogin()
            } else {
                handler.postDelayed(this, SESSION_POLL_INTERVAL_MS)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val selectedLanguage = LanguageManager.getSavedLanguage(this)
        val resolvedLanguage = LanguageManager.resolveAppLanguage(selectedLanguage)
        val strings = LanguageManager.getStrings(selectedLanguage)
        val layoutDirection = if (resolvedLanguage == AppLanguage.AR) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
        statusText = strings.instagramLoginNotice
        val themeMode = runCatching {
            val saved = getSharedPreferences("app_settings", MODE_PRIVATE)
                .getString("theme_mode", ThemeMode.SYSTEM.name)
            ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val current = webView
                    if (current?.canGoBack() == true) {
                        current.goBack()
                    } else {
                        finish()
                    }
                }
            },
        )

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                IGThreadsDLTheme(themeMode = themeMode) {
                    val context = LocalContext.current
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = strings.btnCancel,
                                )
                            }
                            Text(
                                text = strings.instagramLoginTitle,
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = statusText,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                        Spacer(Modifier.padding(top = 2.dp))
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            factory = {
                                WebView(context).apply {
                                    webView = this
                                    val loginWebView = this
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    CookieManager.getInstance().apply {
                                        setAcceptCookie(true)
                                        setAcceptThirdPartyCookies(loginWebView, true)
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                        ): Boolean {
                                            val target = request?.url ?: return true
                                            if (target.scheme != "https" || !isAllowedLoginHost(target.host)) {
                                                statusText = strings.instagramLoginBlockedNavigation
                                                return true
                                            }
                                            return false
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            view?.let(::repairInstagramLoginLayout)
                                            InstagramSessionManager.flush()
                                            if (InstagramSessionManager.isLoggedIn()) {
                                                completeLogin()
                                            }
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?,
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            if (request?.isForMainFrame == true && !completed) {
                                                statusText = strings.instagramLoginLoadFailed
                                            }
                                        }
                                    }
                                    loadUrl(INSTAGRAM_LOGIN_URL)
                                }
                            },
                        )
                    }
                }
            }
        }
        handler.post(sessionPoll)
    }

    /**
     * Instagram's current mobile login shell can give its scroll container a
     * computed height of zero inside Android WebView. The form is present in
     * the DOM but gets clipped after the initial logo frame. This script only
     * repairs those zero-height ancestors; it never reads or submits a field.
     */
    private fun repairInstagramLoginLayout(view: WebView) {
        view.evaluateJavascript(INSTAGRAM_LAYOUT_REPAIR_SCRIPT, null)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroy()
    }

    private fun completeLogin() {
        if (completed) return
        completed = true
        handler.removeCallbacks(sessionPoll)
        val strings = LanguageManager.getStrings(
            LanguageManager.getSavedLanguage(this),
        )
        statusText = strings.instagramLoginSuccess
        InstagramSessionManager.flush()
        setResult(Activity.RESULT_OK, Intent())
        handler.postDelayed(::finish, LOGIN_SUCCESS_DELAY_MS)
    }

    private fun isAllowedLoginHost(host: String?): Boolean {
        val normalized = host?.lowercase() ?: return false
        return allowedLoginHostSuffixes.any { suffix ->
            normalized == suffix || normalized.endsWith(".$suffix")
        }
    }

    private companion object {
        const val INSTAGRAM_LOGIN_URL =
            "https://www.instagram.com/accounts/login/?next=%2F"
        const val SESSION_POLL_INTERVAL_MS = 500L
        const val LOGIN_SUCCESS_DELAY_MS = 800L
        const val INSTAGRAM_LAYOUT_REPAIR_SCRIPT = """
            (() => {
              const marker = 'data-igtdl-height-fix';
              const repair = () => {
                const field = document.querySelector(
                  'input[type="password"], input[name="username"], input'
                );
                if (!field) return;

                const height = Math.max(
                  window.innerHeight || 0,
                  document.documentElement.clientHeight || 0
                );
                if (!height) return;
                const pixelHeight = `${'$'}{height}px`;

                document.documentElement.style.setProperty(
                  'height', pixelHeight, 'important'
                );
                if (document.body) {
                  document.body.style.setProperty(
                    'height', pixelHeight, 'important'
                  );
                }

                let node = field.parentElement;
                while (node && node !== document.documentElement) {
                  if (
                    node.hasAttribute(marker) ||
                    node.getBoundingClientRect().height <= 1
                  ) {
                    node.setAttribute(marker, '');
                    node.style.setProperty('height', pixelHeight, 'important');
                  }
                  node = node.parentElement;
                }
              };

              if (!window.__igtdlLayoutRepairInstalled) {
                window.__igtdlLayoutRepairInstalled = true;
                window.addEventListener('resize', repair, { passive: true });
                [0, 250, 750, 1500, 3000].forEach(delay =>
                  window.setTimeout(repair, delay)
                );
              }
              repair();
            })();
        """
        val allowedLoginHostSuffixes = setOf(
            "instagram.com",
            "facebook.com",
            "accountscenter.com",
        )
    }
}
