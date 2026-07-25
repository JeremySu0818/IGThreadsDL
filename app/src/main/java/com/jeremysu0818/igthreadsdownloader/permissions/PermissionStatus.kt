package com.jeremysu0818.igthreadsdownloader.permissions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.jeremysu0818.igthreadsdownloader.accessibility.AppLaunchAccessibilityService
import com.jeremysu0818.igthreadsdownloader.overlay.OverlayService

data class AppPermissionStatus(
    val overlay: Boolean,
    val notifications: Boolean,
    val accessibility: Boolean,
) {
    val overlayReady: Boolean
        get() = overlay

    val allRequiredGranted: Boolean
        get() = overlay && notifications && accessibility
}

object PermissionStatus {
    fun current(context: Context): AppPermissionStatus = AppPermissionStatus(
        overlay = Settings.canDrawOverlays(context),
        notifications = notificationsEnabled(context),
        accessibility = isAutoLaunchDetectorEnabled(context),
    )

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri(),
        )

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun isAutoLaunchEnabled(context: Context): Boolean =
        context.getSharedPreferences(OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_LAUNCH_TARGET_APPS, true)

    fun setAutoLaunchEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_AUTO_LAUNCH_TARGET_APPS, enabled)
            }
        if (!enabled) stopAutoOverlay(context)
    }

    fun isAutoLaunchDetectorEnabled(context: Context): Boolean {
        val expected = ComponentName(
            context,
            AppLaunchAccessibilityService::class.java,
        )
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()

        return enabledServices
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expected }
    }

    fun startOverlay(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        context.getSharedPreferences(OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_OVERLAY_ENABLED, true)
                putBoolean(KEY_AUTO_OVERLAY_ACTIVE, false)
            }
        launchOverlayService(context)
        return true
    }

    fun startAutoOverlay(context: Context): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        val preferences = context.getSharedPreferences(
            OVERLAY_PREFERENCES,
            Context.MODE_PRIVATE,
        )
        if (preferences.getBoolean(KEY_OVERLAY_SERVICE_ACTIVE, false)) return true

        preferences.edit {
            putBoolean(KEY_AUTO_OVERLAY_ACTIVE, true)
        }
        launchOverlayService(context)
        return true
    }

    fun stopAutoOverlay(context: Context) {
        val preferences = context.getSharedPreferences(
            OVERLAY_PREFERENCES,
            Context.MODE_PRIVATE,
        )
        if (!preferences.getBoolean(KEY_AUTO_OVERLAY_ACTIVE, false)) return

        preferences.edit {
            putBoolean(KEY_AUTO_OVERLAY_ACTIVE, false)
        }
        context.stopService(Intent(context, OverlayService::class.java))
    }

    private fun launchOverlayService(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, OverlayService::class.java),
        )
    }

    fun stopOverlay(context: Context) {
        context.getSharedPreferences(OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_OVERLAY_ENABLED, false)
                putBoolean(KEY_AUTO_OVERLAY_ACTIVE, false)
            }
        context.stopService(Intent(context, OverlayService::class.java))
    }

    fun shouldRunOverlay(context: Context): Boolean =
        context.getSharedPreferences(OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(KEY_OVERLAY_ENABLED, true)

    private fun notificationsEnabled(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    const val OVERLAY_PREFERENCES = "overlay_preferences"
    const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    const val KEY_AUTO_LAUNCH_TARGET_APPS = "auto_launch_target_apps"
    const val KEY_AUTO_OVERLAY_ACTIVE = "auto_overlay_active"
    const val KEY_OVERLAY_SERVICE_ACTIVE = "overlay_service_active"
}
