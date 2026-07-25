package com.jeremysu0818.igthreadsdownloader

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.jeremysu0818.igthreadsdownloader.permissions.AppPermissionStatus
import com.jeremysu0818.igthreadsdownloader.permissions.PermissionStatus
import com.jeremysu0818.igthreadsdownloader.ui.MainScreen
import com.jeremysu0818.igthreadsdownloader.ui.MainViewModel
import com.jeremysu0818.igthreadsdownloader.ui.StartupPermissionDialog
import com.jeremysu0818.igthreadsdownloader.ui.theme.IGThreadsDownloaderTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import com.jeremysu0818.igthreadsdownloader.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var permissionStatus by mutableStateOf(
        AppPermissionStatus(
            overlay = false,
            notifications = false,
            accessibility = false,
        ),
    )
    private var autoLaunchEnabled by mutableStateOf(false)
    private var overlayRunning by mutableStateOf(false)
    private var skipClipboardOnce = false
    private var readClipboardWhenFocused = false
    private val overlayPreferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PermissionStatus.KEY_OVERLAY_SERVICE_ACTIVE) {
                overlayRunning = PermissionStatus.isOverlayRunning(this)
            }
        }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshPermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        autoLaunchEnabled = PermissionStatus.isAutoLaunchEnabled(this)
        overlayRunning = PermissionStatus.isOverlayRunning(this)
        getSharedPreferences(PermissionStatus.OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(overlayPreferenceListener)
        setContent {
            val state by viewModel.state.collectAsState()
            val isDark = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(isDark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
                onDispose {}
            }

            IGThreadsDownloaderTheme(themeMode = state.themeMode) {
                MainScreen(
                    viewModel = viewModel,
                    permissionStatus = permissionStatus,
                    autoLaunchEnabled = autoLaunchEnabled,
                    overlayRunning = overlayRunning,
                    onAutoLaunchChange = { enabled ->
                        PermissionStatus.setAutoLaunchEnabled(this, enabled)
                        autoLaunchEnabled = enabled
                        if (
                            enabled &&
                            !PermissionStatus.isAutoLaunchDetectorEnabled(this)
                        ) {
                            startActivity(PermissionStatus.accessibilitySettingsIntent())
                        }
                    },
                    onStartOverlay = {
                        if (PermissionStatus.startOverlay(this)) {
                            viewModel.clearMessage()
                        }
                        refreshPermissionStatus()
                    },
                    onStopOverlay = {
                        PermissionStatus.stopOverlay(this)
                    },
                    onRequestOverlayPermission = {
                        startActivity(PermissionStatus.overlaySettingsIntent(this))
                    },
                    onPasteClipboard = {
                        clipboardText()?.let {
                            viewModel.receiveExternalText(it, autoResolve = false)
                        }
                    },
                )
                if (!permissionStatus.allRequiredGranted) {
                    StartupPermissionDialog(
                        status = permissionStatus,
                        onRequestOverlay = {
                            startActivity(PermissionStatus.overlaySettingsIntent(this))
                        },
                        onRequestNotifications = ::requestNotificationPermission,
                        onRequestAccessibility = {
                            startActivity(PermissionStatus.accessibilitySettingsIntent())
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
        autoLaunchEnabled = PermissionStatus.isAutoLaunchEnabled(this)
        overlayRunning = PermissionStatus.isOverlayRunning(this)
        if (permissionStatus.allRequiredGranted && PermissionStatus.shouldRunOverlay(this)) {
            PermissionStatus.startOverlay(this)
        }
        if (skipClipboardOnce) {
            skipClipboardOnce = false
        } else {
            readClipboardWhenFocused = true
        }
    }

    override fun onDestroy() {
        getSharedPreferences(PermissionStatus.OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(overlayPreferenceListener)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && readClipboardWhenFocused) {
            readClipboardWhenFocused = false
            clipboardText()?.let {
                viewModel.receiveExternalText(it, autoResolve = true)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val text = when {
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" ->
                intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> intent?.getStringExtra(EXTRA_SOURCE_URL)
        }
        if (!text.isNullOrBlank()) {
            skipClipboardOnce = true
            viewModel.receiveExternalText(text, autoResolve = true)
        }
        intent?.removeExtra(EXTRA_SOURCE_URL)
        if (intent?.action == Intent.ACTION_SEND) {
            intent.action = null
        }
    }

    private fun clipboardText(): String? {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
    }

    private fun requestNotificationPermission() {
        val runtimePermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !runtimePermissionGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(PermissionStatus.notificationSettingsIntent(this))
        }
    }

    private fun refreshPermissionStatus() {
        permissionStatus = PermissionStatus.current(this)
    }

    companion object {
        const val EXTRA_SOURCE_URL = "source_url"
    }
}
