package com.jeremysu0818.igthreadsdl.ui

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
import com.jeremysu0818.igthreadsdl.permissions.AppPermissionStatus
import com.jeremysu0818.igthreadsdl.ui.theme.MatteCard
import com.jeremysu0818.igthreadsdl.ui.theme.MatteTextPrimary
import com.jeremysu0818.igthreadsdl.ui.theme.MatteTextSecondary

import com.jeremysu0818.igthreadsdl.i18n.AppStrings

@Composable
fun StartupPermissionDialog(
    strings: AppStrings,
    status: AppPermissionStatus,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestAccessibility: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(strings.permissionDialogTitle) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(strings.permissionDialogMessage)
                if (!status.overlay) {
                    PermissionButton(
                        text = strings.permissionBtnOverlay,
                        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                        onClick = onRequestOverlay,
                    )
                }
                if (!status.accessibility) {
                    PermissionButton(
                        text = strings.permissionBtnAccessibility,
                        icon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null)
                        },
                        onClick = onRequestAccessibility,
                    )
                }
                if (!status.notifications) {
                    PermissionButton(
                        text = strings.permissionBtnNotifications,
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        onClick = onRequestNotifications,
                    )
                }
            }
        },
        confirmButton = {},
        containerColor = MatteCard,
        titleContentColor = MatteTextPrimary,
        textContentColor = MatteTextSecondary,
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
