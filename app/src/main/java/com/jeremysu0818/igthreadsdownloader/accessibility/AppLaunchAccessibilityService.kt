package com.jeremysu0818.igthreadsdownloader.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.jeremysu0818.igthreadsdownloader.permissions.PermissionStatus

class AppLaunchAccessibilityService : AccessibilityService() {
    private var lastStartAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!PermissionStatus.isAutoLaunchEnabled(this)) return

        val foregroundPackage = event.packageName?.toString() ?: return
        when (
            foregroundWindowAction(
                foregroundPackage = foregroundPackage,
                ownPackage = packageName,
                inputMethodPackages = enabledInputMethodPackages(),
            )
        ) {
            ForegroundWindowAction.IGNORE -> Unit
            ForegroundWindowAction.START -> {
                if (!PermissionStatus.current(this).overlayReady) return

                val now = SystemClock.elapsedRealtime()
                if (now - lastStartAt < START_THROTTLE_MILLIS) return
                lastStartAt = now
                PermissionStatus.startAutoOverlay(this)
            }
            ForegroundWindowAction.STOP -> PermissionStatus.stopAutoOverlay(this)
        }
    }

    override fun onInterrupt() = Unit

    private fun enabledInputMethodPackages(): Set<String> = runCatching {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.enabledInputMethodList
            .mapTo(mutableSetOf()) { it.packageName }
    }.getOrDefault(emptySet())

    private companion object {
        const val START_THROTTLE_MILLIS = 2_000L
    }
}

internal enum class ForegroundWindowAction {
    START,
    STOP,
    IGNORE,
}

internal fun foregroundWindowAction(
    foregroundPackage: String,
    ownPackage: String,
    inputMethodPackages: Set<String>,
): ForegroundWindowAction = when {
    foregroundPackage in targetPackages -> ForegroundWindowAction.START
    foregroundPackage == ownPackage ||
        foregroundPackage in systemWindowPackages ||
        foregroundPackage in inputMethodPackages -> ForegroundWindowAction.IGNORE
    else -> ForegroundWindowAction.STOP
}

private val targetPackages = setOf(
    "com.instagram.android",
    "com.instagram.barcelona",
)

private val systemWindowPackages = setOf(
    "android",
    "com.android.systemui",
    "com.android.permissioncontroller",
    "com.google.android.permissioncontroller",
)
