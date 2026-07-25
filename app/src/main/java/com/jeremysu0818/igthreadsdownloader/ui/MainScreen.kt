package com.jeremysu0818.igthreadsdownloader.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import com.jeremysu0818.igthreadsdownloader.ui.theme.ThemeMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import android.os.Build
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import com.jeremysu0818.igthreadsdownloader.domain.download.DownloadRecord
import com.jeremysu0818.igthreadsdownloader.domain.download.DownloadStatus
import com.jeremysu0818.igthreadsdownloader.domain.model.MediaItem
import com.jeremysu0818.igthreadsdownloader.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdownloader.domain.model.MediaManifest
import com.jeremysu0818.igthreadsdownloader.permissions.AppPermissionStatus
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentAccent
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentBackground
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentBorder
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentSubtleSurface
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentSurface
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentSelection
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentSwitchOff
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentSwitchOn
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentTextMuted
import com.jeremysu0818.igthreadsdownloader.ui.theme.ContentTextPrimary
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteAmber
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteBg
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteCard
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteCardBorder
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteCardHover
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteEmerald
import com.jeremysu0818.igthreadsdownloader.ui.theme.MattePrimary
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteRose
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteTextMuted
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteTextPrimary
import com.jeremysu0818.igthreadsdownloader.ui.theme.MatteTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager

private data class NavItem(
    val tab: MainTab,
    val label: String,
    val icon: ImageVector,
    val count: Int,
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    permissionStatus: AppPermissionStatus,
    autoLaunchEnabled: Boolean,
    onAutoLaunchChange: (Boolean) -> Unit,
    onOverlaySettings: () -> Unit,
    onNotificationPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onPasteClipboard: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val hazeState = remember { HazeState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBg)
            .statusBarsPadding(),
    ) {
             Column(
            modifier = Modifier.fillMaxSize().hazeSource(state = hazeState),
        ) {
            when (state.tab) {
                MainTab.DOWNLOAD -> DownloadPage(
                    state = state,
                    onInputChange = viewModel::updateInput,
                    onParse = viewModel::parse,
                    onPasteClipboard = onPasteClipboard,
                    onToggleItem = viewModel::toggleSelection,
                    onSelectAll = viewModel::selectAll,
                    onDownload = viewModel::downloadSelected,
                )
                MainTab.QUEUE -> RecordsPage(
                    records = state.records.filter { it.isActive },
                    title = "下載佇列",
                    subtitle = "查看進行中的下載工作與即時進度。",
                    emptyTitle = "目前沒有下載中的工作",
                    emptyBody = "在「解析」貼上 IG 或 Threads 連結後，下載進度會顯示在這裡。",
                    onCancel = viewModel::cancel,
                    onRetry = viewModel::retry,
                    onOpen = viewModel::open,
                    onShare = viewModel::share,
                    onDelete = viewModel::delete,
                )
                MainTab.HISTORY -> RecordsPage(
                    records = state.records.filter { !it.isActive },
                    title = "下載紀錄",
                    subtitle = "管理已完成、失敗或取消的檔案。",
                    emptyTitle = "尚無歷史紀錄",
                    emptyBody = "已下載完成或取消的檔案紀錄會存放在這裡。",
                    onCancel = viewModel::cancel,
                    onRetry = viewModel::retry,
                    onOpen = viewModel::open,
                    onShare = viewModel::share,
                    onDelete = viewModel::delete,
                )
                MainTab.SETTINGS -> SettingsPage(
                    permissionStatus = permissionStatus,
                    currentThemeMode = state.themeMode,
                    autoLaunchEnabled = autoLaunchEnabled,
                    onAutoLaunchChange = onAutoLaunchChange,
                    onSelectThemeMode = viewModel::selectThemeMode,
                    onOverlaySettings = onOverlaySettings,
                    onNotificationPermission = onNotificationPermission,
                    onStartOverlay = onStartOverlay,
                    onStopOverlay = onStopOverlay,
                )
            }
        }

        BottomNavBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            hazeState = hazeState,
            selected = state.tab,
            activeCount = state.records.count { it.isActive },
            historyCount = state.records.count { !it.isActive },
            onSelect = viewModel::selectTab,
        )
    }
}


@Composable
private fun BottomNavBar(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    selected: MainTab,
    activeCount: Int,
    historyCount: Int,
    onSelect: (MainTab) -> Unit,
) {
    val items = remember(activeCount, historyCount) {
        listOf(
            NavItem(MainTab.DOWNLOAD, "解析", Icons.Default.Download, 0),
            NavItem(MainTab.QUEUE, "佇列", Icons.Default.Bolt, activeCount),
            NavItem(MainTab.HISTORY, "紀錄", Icons.Default.History, historyCount),
            NavItem(MainTab.SETTINGS, "設定", Icons.Default.Settings, 0),
        )
    }

    val selectedIndex = remember(selected) {
        items.indexOfFirst { it.tab == selected }.coerceAtLeast(0)
    }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = 0.75f,
        ),
        label = "nav_slider_position",
    )

    Box(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.45f),
                ambientColor = Color.Black.copy(alpha = 0.25f),
            )
            .clip(CircleShape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = MatteBg,
                    blurRadius = 15.dp,
                    tint = HazeTint(MatteCard.copy(alpha = 0.78f))
                )
            )
            .border(1.dp, MatteCardBorder, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            // Animated translucent slider pill
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f / items.size)
                        .graphicsLayer {
                            translationX = animatedIndex * size.width
                        }
                        .clip(CircleShape)
                        .background(MatteCardHover.copy(alpha = 0.55f))
                )
            }

            // Foreground items Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { navItem ->
                    val isSelected = selected == navItem.tab

                    val animatedIconTint by animateColorAsState(
                        targetValue = if (isSelected) MattePrimary else MatteTextMuted,
                        label = "icon_tint"
                    )
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) MatteTextPrimary else MatteTextMuted,
                        label = "text_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(navItem.tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = navItem.icon,
                                contentDescription = navItem.label,
                                tint = animatedIconTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                navItem.label,
                                color = animatedTextColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                            if (navItem.count > 0) {
                                Spacer(Modifier.width(5.dp))
                                val animatedBadgeBg by animateColorAsState(
                                    targetValue = if (isSelected) MattePrimary else MatteCardBorder,
                                    label = "badge_bg"
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(animatedBadgeBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${navItem.count}",
                                        color = MatteTextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadPage(
    state: MainUiState,
    onInputChange: (String) -> Unit,
    onParse: () -> Unit,
    onPasteClipboard: () -> Unit,
    onToggleItem: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onDownload: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 112.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            ContentFrame {
                PageHeader(
                    eyebrow = "IG + THREADS",
                    title = "儲存公開貼文",
                    subtitle = "貼上連結、確認媒體，再直接下載到裝置。",
                )
            }
        }
        item {
            ContentFrame {
                UrlInputCard(
                    value = state.input,
                    isResolving = state.isResolving,
                    onValueChange = onInputChange,
                    onPaste = onPasteClipboard,
                    onParse = onParse,
                )
            }
        }
        state.errorMessage?.let { message ->
            item { ContentFrame { SimpleBanner(message, MatteRose) } }
        }
        state.noticeMessage?.let { message ->
            item { ContentFrame { SimpleBanner(message, MatteEmerald) } }
        }
        state.manifest?.let { manifest ->
            item {
                ContentFrame {
                    ManifestCard(
                        manifest = manifest,
                        selectedIds = state.selectedIds,
                        onToggleItem = onToggleItem,
                        onSelectAll = onSelectAll,
                        onDownload = onDownload,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsCard(
    status: AppPermissionStatus,
    onOverlaySettings: () -> Unit,
    onNotificationPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    Column {
        SectionTitle(
            title = "快速設定",
            subtitle = "管理懸浮工具與下載通知。",
        )
        Spacer(Modifier.height(10.dp))
        SectionSurface {
            SettingsRow(
                icon = Icons.Default.Tune,
                title = "懸浮視窗",
                subtitle = if (status.overlay) "權限已開啟" else "需要系統顯示權限",
                enabled = status.overlay,
                onClick = onOverlaySettings,
            )
            ContentDivider()
            SettingsRow(
                icon = Icons.Default.Info,
                title = "下載通知",
                subtitle = if (status.notifications) "通知已允許" else "完成與錯誤提醒尚未開啟",
                enabled = status.notifications,
                onClick = onNotificationPermission,
            )
            ContentDivider()
            SettingsRow(
                icon = Icons.Default.Folder,
                title = "儲存目錄",
                subtitle = "系統 Downloads 與媒體庫",
                enabled = true,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HighRadiusButton(
                text = "啟動懸浮工具",
                icon = Icons.Default.PlayArrow,
                primary = true,
                enabled = status.overlayReady,
                modifier = Modifier.weight(1f),
                onClick = onStartOverlay,
            )
            HighRadiusButton(
                text = "關閉懸浮窗",
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onStopOverlay,
            )
        }
    }
}

@Composable
private fun ThemeModeCard(
    currentMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
) {
    Column {
        SectionTitle(
            title = "外觀",
            subtitle = "選擇最適合目前環境的顯示模式。",
        )
        Spacer(Modifier.height(10.dp))
        SectionSurface {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ThemeOptionChip(
                    label = "跟隨系統",
                    selected = currentMode == ThemeMode.SYSTEM,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(ThemeMode.SYSTEM) },
                )
                ThemeOptionChip(
                    label = "淺色",
                    selected = currentMode == ThemeMode.LIGHT,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(ThemeMode.LIGHT) },
                )
                ThemeOptionChip(
                    label = "深色",
                    selected = currentMode == ThemeMode.DARK,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(ThemeMode.DARK) },
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) ContentSubtleSurface else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) ContentTextPrimary else ContentTextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsPage(
    permissionStatus: AppPermissionStatus,
    currentThemeMode: ThemeMode,
    autoLaunchEnabled: Boolean,
    onAutoLaunchChange: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onOverlaySettings: () -> Unit,
    onNotificationPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
) {
    var showColorModeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                top = 8.dp,
                end = 14.dp,
                bottom = 112.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Settings",
                        color = ContentTextPrimary,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AiSettingsRow(
                        icon = Icons.Default.Palette,
                        title = "Color mode",
                        subtitle = currentThemeMode.displayName(),
                        onClick = { showColorModeDialog = true },
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AutoLaunchSettingsRow(
                        checked = autoLaunchEnabled,
                        onCheckedChange = onAutoLaunchChange,
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AiSettingsRow(
                        icon = Icons.Default.Tune,
                        title = "懸浮視窗",
                        subtitle = if (permissionStatus.overlay) "已允許" else "需要權限",
                        trailingText = if (permissionStatus.overlay) "On" else "Off",
                        onClick = onOverlaySettings,
                    )
                    AiSettingsDivider()
                    AiSettingsRow(
                        icon = Icons.Default.Info,
                        title = "下載通知",
                        subtitle = if (permissionStatus.notifications) "已允許" else "尚未開啟",
                        trailingText = if (permissionStatus.notifications) "On" else "Off",
                        onClick = onNotificationPermission,
                    )
                    AiSettingsDivider()
                    AiSettingsRow(
                        icon = Icons.Default.Folder,
                        title = "儲存位置",
                        subtitle = "Downloads / 媒體庫",
                        trailingText = "自動",
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AiSettingsRow(
                        icon = Icons.Default.PlayArrow,
                        title = "啟動懸浮工具",
                        subtitle = if (permissionStatus.overlayReady) "可立即啟動" else "請先完成權限設定",
                        enabled = permissionStatus.overlayReady,
                        onClick = onStartOverlay,
                    )
                    AiSettingsDivider()
                    AiSettingsRow(
                        icon = Icons.Default.Close,
                        title = "關閉懸浮工具",
                        subtitle = "停止目前顯示的懸浮視窗",
                        onClick = onStopOverlay,
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AiSettingsRow(
                        icon = Icons.Default.Info,
                        title = "IG & Threads 下載器",
                        subtitle = "儲存公開圖片與影片",
                        trailingText = "1.0",
                    )
                }
            }
        }
    }

    if (showColorModeDialog) {
        ColorModeDialog(
            selectedMode = currentThemeMode,
            onSelectMode = {
                onSelectThemeMode(it)
                showColorModeDialog = false
            },
            onDismiss = { showColorModeDialog = false },
        )
    }
}

@Composable
private fun AutoLaunchSettingsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = ContentTextPrimary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "自動啟動懸浮工具",
                color = ContentTextPrimary,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "開啟 Instagram 或 Threads 時自動顯示",
                color = ContentTextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        ReferenceSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ReferenceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) ContentSwitchOn else ContentSwitchOff)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun AiSettingsGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ContentSurface),
        content = content,
    )
}

@Composable
private fun AiSettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ContentBackground),
    )
}

@Composable
private fun AiSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    var modifier = Modifier
        .fillMaxWidth()
        .height(if (subtitle == null) 68.dp else 76.dp)
    if (onClick != null) {
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    }

    Row(
        modifier = modifier.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) ContentTextPrimary else ContentTextMuted.copy(alpha = 0.55f),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) ContentTextPrimary else ContentTextMuted.copy(alpha = 0.55f),
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = ContentTextMuted,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingText != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailingText,
                color = ContentTextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ColorModeDialog(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as DialogWindowProvider).window
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.60f)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.79f)
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ContentBackground)
                    .padding(start = 20.dp, top = 20.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Text(
                    text = "Color mode",
                    color = ContentTextPrimary,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                ColorModeOption(
                    icon = Icons.Default.BrightnessAuto,
                    label = "System",
                    selected = selectedMode == ThemeMode.SYSTEM,
                    onClick = { onSelectMode(ThemeMode.SYSTEM) },
                )
                ColorModeOption(
                    icon = Icons.Default.LightMode,
                    label = "Light",
                    selected = selectedMode == ThemeMode.LIGHT,
                    onClick = { onSelectMode(ThemeMode.LIGHT) },
                )
                ColorModeOption(
                    icon = Icons.Default.DarkMode,
                    label = "Dark",
                    selected = selectedMode == ThemeMode.DARK,
                    onClick = { onSelectMode(ThemeMode.DARK) },
                )
            }
        }
    }
}

@Composable
private fun ColorModeOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) ContentSelection else ContentTextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(25.dp),
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = label,
            color = color,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = ContentSelection,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
private fun StatusChip(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(ContentSubtleSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (enabled) MatteEmerald else MatteAmber),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = ContentTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun UrlInputCard(
    value: String,
    isResolving: Boolean,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onParse: () -> Unit,
) {
    Column {
        SectionTitle(
            title = "貼文連結",
            subtitle = "支援 Instagram 貼文、Reels 與 Threads 公開貼文。",
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(ContentSurface)
                .border(1.dp, ContentBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = ContentAccent,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        textStyle = TextStyle(
                            color = ContentTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        minLines = 2,
                        maxLines = 3,
                        cursorBrush = SolidColor(ContentAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onParse() }),
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isBlank()) {
                                    Text(
                                        "https://www.instagram.com/p/…",
                                        color = ContentTextMuted,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
                ContentDivider()
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HighRadiusButton(
                        text = "貼上",
                        icon = Icons.Default.ContentPaste,
                        primary = false,
                        modifier = Modifier.weight(1f),
                        onClick = onPaste,
                    )
                    HighRadiusButton(
                        text = if (isResolving) "解析中…" else "解析連結",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        primary = true,
                        enabled = !isResolving,
                        modifier = Modifier.weight(1f),
                        onClick = onParse,
                    )
                }
            }
        }
    }
}

@Composable
private fun ManifestCard(
    manifest: MediaManifest,
    selectedIds: Set<String>,
    onToggleItem: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onDownload: () -> Unit,
) {
    Column {
        SectionTitle(
            title = "媒體預覽",
            subtitle = "${manifest.platform.value.uppercase()} · ${manifest.author?.let { "@$it" } ?: "公開來源"}",
            trailing = "${manifest.items.size} 個項目",
        )
        manifest.thumbnailUrl?.let {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = it,
                contentDescription = "媒體預覽",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.78f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ContentSubtleSurface),
                contentScale = ContentScale.Crop,
            )
        }

        manifest.caption?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = ContentTextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        manifest.warnings.forEach {
            Spacer(Modifier.height(8.dp))
            SimpleBanner(it, MatteAmber)
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "選擇要下載的媒體",
                    color = ContentTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "預估大小 ${estimatedSize(manifest.items)}",
                    color = ContentTextMuted,
                    fontSize = 11.sp,
                )
            }
            HighRadiusButton(
                text = if (selectedIds.size == manifest.items.size) "取消全選" else "全選",
                primary = false,
                onClick = { onSelectAll(selectedIds.size != manifest.items.size) },
            )
        }

        Spacer(Modifier.height(10.dp))
        SectionSurface {
            manifest.items.forEachIndexed { index, item ->
                MediaItemRow(
                    item = item,
                    index = index,
                    selected = item.id in selectedIds,
                    onToggle = { onToggleItem(item.id) },
                )
                if (index != manifest.items.lastIndex) ContentDivider()
            }
        }

        Spacer(Modifier.height(14.dp))

        HighRadiusButton(
            text = "下載選取的 ${selectedIds.size} 個項目",
            icon = Icons.Default.Download,
            primary = true,
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = onDownload,
        )
    }
}

@Composable
private fun MediaItemRow(
    item: MediaItem,
    index: Int,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) ContentSubtleSurface else Color.Transparent)
            .clickable(onClick = onToggle)
            .padding(vertical = 13.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleCheckCircle(selected)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.type == MediaItemType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.Image,
                    contentDescription = null,
                    tint = ContentTextMuted,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "#${index + 1} ${if (item.type == MediaItemType.VIDEO) "影片" else "圖片"}",
                    color = ContentTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                "${item.mimeType ?: "檔案"}  ·  ${formatBytes(item.contentLength)}",
                color = ContentTextMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun RecordsPage(
    records: List<DownloadRecord>,
    title: String,
    subtitle: String,
    emptyTitle: String,
    emptyBody: String,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onOpen: (DownloadRecord) -> Unit,
    onShare: (DownloadRecord) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ContentFrame {
                PageHeader(
                    eyebrow = if (records.isEmpty()) "LIBRARY" else "${records.size} ITEMS",
                    title = title,
                    subtitle = subtitle,
                )
            }
        }
        if (records.isEmpty()) {
            item {
                ContentFrame {
                    EmptyRecordsState(
                        title = emptyTitle,
                        body = emptyBody,
                    )
                }
            }
        }
        items(records, key = { it.managerId }) { record ->
            ContentFrame {
                RecordCard(
                    record = record,
                    onCancel = { onCancel(record.managerId) },
                    onRetry = { onRetry(record.managerId) },
                    onOpen = { onOpen(record) },
                    onShare = { onShare(record) },
                    onDelete = { onDelete(record.managerId) },
                )
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: DownloadRecord,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    SectionSurface {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(statusColor(record.status).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (record.mediaType == MediaItemType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.Image,
                    contentDescription = null,
                    tint = statusColor(record.status),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    record.filename,
                    color = ContentTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${record.platform.value}  ·  ${formatDate(record.updatedAt)}",
                    color = ContentTextMuted,
                    fontSize = 11.sp,
                )
            }
            SimpleBadge(statusLabel(record.status), statusColor(record.status))
        }

        val progress = record.progress
        if (progress != null && record.isActive) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ContentSubtleSurface),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(ContentAccent),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            buildString {
                append(formatBytes(record.bytesDownloaded))
                record.totalBytes?.let { append(" / ${formatBytes(it)}") }
                record.statusMessage?.let { append("  ·  $it") }
            },
            color = if (record.status == DownloadStatus.FAILED) MatteRose else ContentTextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                record.isActive -> HighRadiusButton("取消", icon = Icons.Default.Close, primary = false, onClick = onCancel)
                record.status == DownloadStatus.SUCCEEDED -> {
                    HighRadiusButton("開啟檔案", icon = Icons.Default.PlayArrow, primary = true, onClick = onOpen)
                    HighRadiusButton("分享", icon = Icons.Default.Share, primary = false, onClick = onShare)
                    HighRadiusButton("刪除檔案", icon = Icons.Default.Delete, primary = false, onClick = onDelete)
                }
                else -> {
                    HighRadiusButton("重試", icon = Icons.Default.Refresh, primary = true, onClick = onRetry)
                    HighRadiusButton("刪除紀錄", icon = Icons.Default.Delete, primary = false, onClick = onDelete)
                }
            }
        }
        }
    }
}

@Composable
private fun ContentFrame(
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            content = content,
        )
    }
}

@Composable
private fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
) {
    Column {
        Text(
            text = eyebrow,
            color = ContentAccent,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = ContentTextPrimary,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5f).sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = ContentTextMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = ContentTextPrimary,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = ContentTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailing,
                color = ContentAccent,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionSurface(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ContentSurface)
            .border(1.dp, ContentBorder, RoundedCornerShape(22.dp)),
        content = content,
    )
}

@Composable
private fun ContentDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(ContentBorder.copy(alpha = 0.72f)),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    var rowModifier = Modifier.fillMaxWidth()
    if (onClick != null) rowModifier = rowModifier.clickable(onClick = onClick)

    Row(
        modifier = rowModifier.padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(ContentSubtleSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ContentTextPrimary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = ContentTextPrimary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = ContentTextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (trailingText != null) {
            Text(
                text = trailingText,
                color = ContentTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (enabled) MatteEmerald else MatteAmber),
            )
        }
    }
}

@Composable
private fun EmptyRecordsState(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 76.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ContentSubtleSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = ContentTextMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = ContentTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            color = ContentTextMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

// -----------------------------------------------------------------------------
// REUSABLE MINIMALIST MATTE COMPONENTS
// -----------------------------------------------------------------------------

@Composable
private fun MatteCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(38.dp))
            .background(MatteCard)
            .border(1.dp, MatteCardBorder, RoundedCornerShape(38.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun HighRadiusButton(
    text: String,
    icon: ImageVector? = null,
    primary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                when {
                    !enabled -> ContentSubtleSurface
                    primary -> ContentAccent
                    else -> ContentSubtleSurface
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) ContentTextPrimary else ContentTextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                color = if (enabled) ContentTextPrimary else ContentTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SimpleStatusPill(text: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (enabled) MatteEmerald.copy(alpha = 0.12f) else MatteCardHover)
            .border(1.dp, if (enabled) MatteEmerald.copy(alpha = 0.3f) else MatteCardBorder, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (enabled) MatteEmerald else MatteAmber)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            color = if (enabled) MatteEmerald else MatteTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SimpleBadge(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SimpleCheckCircle(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) ContentAccent else Color.Transparent)
            .border(
                1.5.dp,
                if (selected) ContentAccent else ContentTextMuted,
                CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ContentTextPrimary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun SimpleBanner(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun statusColor(status: DownloadStatus): Color = when (status) {
    DownloadStatus.QUEUED, DownloadStatus.RUNNING, DownloadStatus.PAUSED -> MattePrimary
    DownloadStatus.SUCCEEDED -> MatteEmerald
    DownloadStatus.FAILED -> MatteRose
    DownloadStatus.CANCELLED -> MatteTextMuted
}

private fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.QUEUED -> "等待中"
    DownloadStatus.RUNNING -> "下載中"
    DownloadStatus.PAUSED -> "已暫停"
    DownloadStatus.SUCCEEDED -> "已完成"
    DownloadStatus.FAILED -> "下載失敗"
    DownloadStatus.CANCELLED -> "已取消"
}

private fun estimatedSize(items: List<MediaItem>): String =
    if (items.all { it.contentLength != null }) {
        formatBytes(items.sumOf { it.contentLength ?: 0L })
    } else {
        "未知"
    }

private fun formatBytes(bytes: Long?): String {
    bytes ?: return "未知大小"
    return when {
        bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN).format(Date(timestamp))
