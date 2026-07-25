package com.jeremysu0818.igthreadsdownloader.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.jeremysu0818.igthreadsdownloader.permissions.PermissionStatus

class AppLaunchAccessibilityService : AccessibilityService() {
    private var lastStartAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!PermissionStatus.isAutoLaunchEnabled(this)) return

        val foregroundPackage = event.packageName?.toString() ?: return
        if (foregroundPackage == packageName || foregroundPackage in SYSTEM_WINDOW_PACKAGES) {
            return
        }

        if (foregroundPackage in TARGET_PACKAGES) {
            if (!PermissionStatus.current(this).overlayReady) return

            val now = SystemClock.elapsedRealtime()
            if (now - lastStartAt < START_THROTTLE_MILLIS) return
            lastStartAt = now
            PermissionStatus.startAutoOverlay(this)
        } else {
            PermissionStatus.stopAutoOverlay(this)
        }
    }

    override fun onInterrupt() = Unit

    private companion object {
        const val START_THROTTLE_MILLIS = 2_000L

        val TARGET_PACKAGES = setOf(
            "com.instagram.android",
            "com.instagram.barcelona",
        )

        val SYSTEM_WINDOW_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )
    }
}
