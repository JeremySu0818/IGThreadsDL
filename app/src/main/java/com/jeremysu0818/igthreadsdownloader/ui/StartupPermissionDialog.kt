package com.jeremysu0818.igthreadsdownloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jeremysu0818.igthreadsdownloader.permissions.AppPermissionStatus

@Composable
fun StartupPermissionDialog(
    status: AppPermissionStatus,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestAccessibility: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("請先完成權限設定") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("以下權限是 App 運作的必要條件，全部開啟後即可開始使用。")
                if (!status.overlay) {
                    PermissionButton(
                        text = "開啟懸浮視窗",
                        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                        onClick = onRequestOverlay,
                    )
                }
                if (!status.accessibility) {
                    PermissionButton(
                        text = "開啟無障礙服務",
                        icon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null)
                        },
                        onClick = onRequestAccessibility,
                    )
                }
                if (!status.notifications) {
                    PermissionButton(
                        text = "開啟下載通知",
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = onRequestNotifications,
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun PermissionButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        icon()
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
