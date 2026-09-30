package com.jeremysu0818.igthreadsdl.quicksettings

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager
import com.jeremysu0818.igthreadsdl.MainActivity
import com.jeremysu0818.igthreadsdl.permissions.PermissionStatus

class OverlayTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!PermissionStatus.current(this).allRequiredGranted) {
            openPermissionSetup()
            return
        }

        if (PermissionStatus.isOverlayRunning(this)) {
            PermissionStatus.stopOverlay(this)
        } else {
            PermissionStatus.startOverlay(this)
        }
        updateTile()
    }

    private fun updateTile() {
        val strings = LanguageManager.getStrings(LanguageManager.getSavedLanguage(this))
        qsTile?.apply {
            label = strings.appName
            subtitle = if (PermissionStatus.isOverlayRunning(this@OverlayTileService)) {
                strings.overlayQuickControlRunning
            } else {
                strings.overlayQuickControlTapToStart
            }
            state = if (PermissionStatus.isOverlayRunning(this@OverlayTileService)) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            updateTile()
        }
    }

    @Suppress("DEPRECATION")
    private fun openPermissionSetup() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
