package com.jeremysu0818.igthreadsdl.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPictureAlt
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
import androidx.compose.material.icons.filled.Language
import com.jeremysu0818.igthreadsdl.i18n.AppLanguage
import com.jeremysu0818.igthreadsdl.i18n.AppStrings
import androidx.compose.runtime.collectAsState
import com.jeremysu0818.igthreadsdl.ui.theme.ThemeMode
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
import com.jeremysu0818.igthreadsdl.domain.download.DownloadRecord
import com.jeremysu0818.igthreadsdl.domain.download.DownloadStatus
import com.jeremysu0818.igthreadsdl.domain.model.MediaItem
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.model.MediaManifest
import com.jeremysu0818.igthreadsdl.permissions.AppPermissionStatus
import com.jeremysu0818.igthreadsdl.ui.theme.ContentAccent
import com.jeremysu0818.igthreadsdl.ui.theme.ContentBackground
import com.jeremysu0818.igthreadsdl.ui.theme.ContentBorder
import com.jeremysu0818.igthreadsdl.ui.theme.ContentSubtleSurface
import com.jeremysu0818.igthreadsdl.ui.theme.ContentSurface
import com.jeremysu0818.igthreadsdl.ui.theme.ContentSelection
import com.jeremysu0818.igthreadsdl.ui.theme.ContentSwitchOff
import com.jeremysu0818.igthreadsdl.ui.theme.ContentSwitchOn
import com.jeremysu0818.igthreadsdl.ui.theme.ContentTextMuted
import com.jeremysu0818.igthreadsdl.ui.theme.ContentTextPrimary
import com.jeremysu0818.igthreadsdl.ui.theme.MatteAmber
import com.jeremysu0818.igthreadsdl.ui.theme.MatteBg
import com.jeremysu0818.igthreadsdl.ui.theme.MatteCard
import com.jeremysu0818.igthreadsdl.ui.theme.MatteCardBorder
import com.jeremysu0818.igthreadsdl.ui.theme.MatteCardHover
import com.jeremysu0818.igthreadsdl.ui.theme.MatteEmerald
import com.jeremysu0818.igthreadsdl.ui.theme.MattePrimary
import com.jeremysu0818.igthreadsdl.ui.theme.MatteRose
import com.jeremysu0818.igthreadsdl.ui.theme.MatteTextMuted
import com.jeremysu0818.igthreadsdl.ui.theme.MatteTextPrimary
import com.jeremysu0818.igthreadsdl.ui.theme.MatteTextSecondary
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
    overlayRunning: Boolean,
    onAutoLaunchChange: (Boolean) -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onPasteClipboard: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val hazeState = remember { HazeState() }
    val strings = state.strings

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
                    strings = strings,
                    onInputChange = viewModel::updateInput,
                    onParse = viewModel::parse,
                    onPasteClipboard = onPasteClipboard,
                    onToggleItem = viewModel::toggleSelection,
                    onSelectAll = viewModel::selectAll,
                    onDownload = viewModel::downloadSelected,
                    overlayReady = permissionStatus.overlayReady,
                    overlayRunning = overlayRunning,
                    onStartOverlay = onStartOverlay,
                    onStopOverlay = onStopOverlay,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                )
                MainTab.QUEUE -> RecordsPage(
                    records = state.records.filter { it.isActive },
                    title = strings.queueTitle,
                    subtitle = strings.queueSubtitle,
                    emptyTitle = strings.queueEmptyTitle,
                    emptyBody = strings.queueEmptyBody,
                    strings = strings,
                    onCancel = viewModel::cancel,
                    onRetry = viewModel::retry,
                    onOpen = viewModel::open,
                    onShare = viewModel::share,
                    onDelete = viewModel::delete,
                )
                MainTab.HISTORY -> RecordsPage(
                    records = state.records.filter { !it.isActive },
                    title = strings.historyTitle,
                    subtitle = strings.historySubtitle,
                    emptyTitle = strings.historyEmptyTitle,
                    emptyBody = strings.historyEmptyBody,
                    strings = strings,
                    onCancel = viewModel::cancel,
                    onRetry = viewModel::retry,
                    onOpen = viewModel::open,
                    onShare = viewModel::share,
                    onDelete = viewModel::delete,
                )
                MainTab.SETTINGS -> SettingsPage(
                    currentThemeMode = state.themeMode,
                    currentAppLanguage = state.appLanguage,
                    strings = strings,
                    autoLaunchEnabled = autoLaunchEnabled,
                    onAutoLaunchChange = onAutoLaunchChange,
                    onSelectThemeMode = viewModel::selectThemeMode,
                    onSelectAppLanguage = viewModel::selectAppLanguage,
                )
            }
        }

        BottomNavBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            hazeState = hazeState,
            selected = state.tab,
            strings = strings,
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
    strings: AppStrings,
    activeCount: Int,
    historyCount: Int,
    onSelect: (MainTab) -> Unit,
) {
    val items = remember(activeCount, historyCount, strings) {
        listOf(
            NavItem(MainTab.DOWNLOAD, strings.navHome, Icons.Default.Download, 0),
            NavItem(MainTab.QUEUE, strings.navQueue, Icons.Default.Bolt, activeCount),
            NavItem(MainTab.HISTORY, strings.navHistory, Icons.Default.History, historyCount),
            NavItem(MainTab.SETTINGS, strings.navSettings, Icons.Default.Settings, 0),
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
    strings: AppStrings,
    onInputChange: (String) -> Unit,
    onParse: () -> Unit,
    onPasteClipboard: () -> Unit,
    onToggleItem: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onDownload: () -> Unit,
    overlayReady: Boolean,
    overlayRunning: Boolean,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground),
        contentPadding = PaddingValues(
            start = 14.dp,
            top = 8.dp,
            end = 14.dp,
            bottom = 112.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            RecordsPageHeader(
                title = strings.pageHomeTitle,
                subtitle = null,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        item {
            ContentFrame {
                OverlayQuickControlCard(
                    strings = strings,
                    overlayReady = overlayReady,
                    overlayRunning = overlayRunning,
                    onStartOverlay = onStartOverlay,
                    onStopOverlay = onStopOverlay,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                )
            }
        }
        item {
            ContentFrame {
                UrlInputCard(
                    value = state.input,
                    isResolving = state.isResolving,
                    strings = strings,
                    onValueChange = onInputChange,
                    onPaste = onPasteClipboard,
                    onParse = onParse,
                )
            }
        }
        state.errorMessage?.let { message ->
            item {
                ContentFrame {
                    AiSettingsGroup {
                        CompactStatusMessage(message, MatteRose)
                    }
                }
            }
        }
        state.noticeMessage?.let { message ->
            item {
                ContentFrame {
                    AiSettingsGroup {
                        CompactStatusMessage(message, MatteEmerald)
                    }
                }
            }
        }
        state.manifest?.let { manifest ->
            item {
                ContentFrame {
                    AiSettingsGroup {
                        Box(Modifier.padding(18.dp)) {
                            ManifestCard(
                                manifest = manifest,
                                selectedIds = state.selectedIds,
                                strings = strings,
                                onToggleItem = onToggleItem,
                                onSelectAll = onSelectAll,
                                onDownload = onDownload,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayQuickControlCard(
    strings: AppStrings,
    overlayReady: Boolean,
    overlayRunning: Boolean,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
) {
    val onToggle = {
        when {
            !overlayReady -> onRequestOverlayPermission()
            overlayRunning -> onStopOverlay()
            else -> onStartOverlay()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ContentSurface)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(ContentSubtleSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PictureInPictureAlt,
                contentDescription = null,
                tint = if (overlayRunning) ContentAccent else ContentTextMuted,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.overlayQuickControlTitle,
                color = ContentTextPrimary,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    !overlayReady -> strings.overlayQuickControlNeedPermission
                    overlayRunning -> strings.overlayQuickControlRunning
                    else -> strings.overlayQuickControlTapToStart
                },
                color = ContentTextMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        ReferenceSwitch(
            checked = overlayRunning,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun SettingsPage(
    currentThemeMode: ThemeMode,
    currentAppLanguage: AppLanguage,
    strings: AppStrings,
    autoLaunchEnabled: Boolean,
    onAutoLaunchChange: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAppLanguage: (AppLanguage) -> Unit,
) {
    var showColorModeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                CompactPageHeader(strings.settingsTitle)
            }
            item {
                AiSettingsGroup {
                    AiSettingsRow(
                        icon = Icons.Default.Palette,
                        title = strings.settingsColorModeTitle,
                        subtitle = currentThemeMode.displayName(strings),
                        onClick = { showColorModeDialog = true },
                    )
                    AiSettingsDivider()
                    AiSettingsRow(
                        icon = Icons.Default.Language,
                        title = strings.settingsLanguageTitle,
                        subtitle = if (currentAppLanguage == AppLanguage.SYSTEM) {
                            "${strings.settingsThemeSystem} (${currentAppLanguage.endonym})"
                        } else {
                            currentAppLanguage.endonym
                        },
                        onClick = { showLanguageDialog = true },
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AutoLaunchSettingsRow(
                        strings = strings,
                        checked = autoLaunchEnabled,
                        onCheckedChange = onAutoLaunchChange,
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AiSettingsRow(
                        icon = Icons.Default.Folder,
                        title = strings.settingsStorageLocationTitle,
                        subtitle = strings.settingsStorageLocationSubtitle,
                        trailingText = strings.settingsStorageLocationAuto,
                    )
                }
            }
            item {
                AiSettingsGroup {
                    AiSettingsRow(
                        icon = Icons.Default.Info,
                        title = strings.settingsAppInfoTitle,
                        subtitle = strings.settingsAppInfoSubtitle,
                        trailingText = "1.0",
                    )
                }
            }
        }
    }

    if (showColorModeDialog) {
        ColorModeDialog(
            selectedMode = currentThemeMode,
            strings = strings,
            onSelectMode = {
                onSelectThemeMode(it)
                showColorModeDialog = false
            },
            onDismiss = { showColorModeDialog = false },
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selectedLanguage = currentAppLanguage,
            strings = strings,
            onSelectLanguage = {
                onSelectAppLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun AutoLaunchSettingsRow(
    strings: AppStrings,
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
                text = strings.settingsAutoLaunchTitle,
                color = ContentTextPrimary,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = strings.settingsAutoLaunchSubtitle,
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
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "switchThumbOffset",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) ContentSwitchOn else ContentSwitchOff,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchBackgroundColor",
    )

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
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
    strings: AppStrings,
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
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.79f)
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ContentBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(start = 20.dp, top = 20.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Text(
                    text = strings.settingsColorModeTitle,
                    color = ContentTextPrimary,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                ColorModeOption(
                    icon = Icons.Default.BrightnessAuto,
                    label = strings.settingsThemeSystem,
                    selected = selectedMode == ThemeMode.SYSTEM,
                    onClick = { onSelectMode(ThemeMode.SYSTEM) },
                )
                ColorModeOption(
                    icon = Icons.Default.LightMode,
                    label = strings.settingsThemeLight,
                    selected = selectedMode == ThemeMode.LIGHT,
                    onClick = { onSelectMode(ThemeMode.LIGHT) },
                )
                ColorModeOption(
                    icon = Icons.Default.DarkMode,
                    label = strings.settingsThemeDark,
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
                contentDescription = null,
                tint = ContentSelection,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun LanguageDialog(
    selectedLanguage: AppLanguage,
    strings: AppStrings,
    onSelectLanguage: (AppLanguage) -> Unit,
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
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 360.dp)
                    .heightIn(max = 520.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ContentBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(start = 20.dp, top = 20.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Text(
                    text = strings.settingsLanguageTitle,
                    color = ContentTextPrimary,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    itemsIndexed(AppLanguage.values()) { _, language ->
                        val label = if (language == AppLanguage.SYSTEM) {
                            "${strings.settingsThemeSystem} (${language.endonym})"
                        } else {
                            language.endonym
                        }
                        LanguageOption(
                            label = label,
                            selected = selectedLanguage == language,
                            onClick = { onSelectLanguage(language) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
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
                contentDescription = null,
                tint = ContentSelection,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun ThemeMode.displayName(strings: AppStrings): String = when (this) {
    ThemeMode.SYSTEM -> strings.settingsThemeSystem
    ThemeMode.LIGHT -> strings.settingsThemeLight
    ThemeMode.DARK -> strings.settingsThemeDark
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
private fun UrlInputCard(
    value: String,
    isResolving: Boolean,
    strings: AppStrings,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onParse: () -> Unit,
) {
    AiSettingsGroup {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = ContentTextPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = strings.urlInputTitle,
                        color = ContentTextPrimary,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = strings.urlInputSubtitle,
                        color = ContentTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ContentBackground)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                strings.urlInputPlaceholder,
                                color = ContentTextMuted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HighRadiusButton(
                    text = strings.urlInputPasteBtn,
                    icon = Icons.Default.ContentPaste,
                    primary = false,
                    modifier = Modifier.weight(1f),
                    onClick = onPaste,
                )
                HighRadiusButton(
                    text = if (isResolving) strings.urlInputParsingBtn else strings.urlInputParseBtn,
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

@Composable
private fun ManifestCard(
    manifest: MediaManifest,
    selectedIds: Set<String>,
    strings: AppStrings,
    onToggleItem: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onDownload: () -> Unit,
) {
    Column {
        SectionTitle(
            title = strings.manifestTitle,
            subtitle = "${manifest.platform.value.uppercase()} · ${manifest.author?.let { "@$it" } ?: strings.manifestPublicSource}",
            trailing = String.format(strings.manifestItemsCount, manifest.items.size),
        )
        manifest.thumbnailUrl?.let {
            Spacer(Modifier.height(12.dp))
            AsyncImage(
                model = it,
                contentDescription = strings.manifestTitle,
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

        if (manifest.isPartial) {
            Spacer(Modifier.height(8.dp))
            SimpleBanner(strings.msgPartialManifest, MatteAmber)
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    strings.manifestSelectMediaTitle,
                    color = ContentTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    String.format(strings.manifestEstimatedSize, estimatedSize(manifest.items, strings)),
                    color = ContentTextMuted,
                    fontSize = 11.sp,
                )
            }
            HighRadiusButton(
                text = if (selectedIds.size == manifest.items.size) strings.manifestDeselectAll else strings.manifestSelectAll,
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
                    strings = strings,
                    onToggle = { onToggleItem(item.id) },
                )
                if (index != manifest.items.lastIndex) ContentDivider()
            }
        }

        Spacer(Modifier.height(14.dp))

        HighRadiusButton(
            text = String.format(strings.manifestDownloadSelectedBtn, selectedIds.size),
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
    strings: AppStrings,
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
                val kind = if (item.type == MediaItemType.VIDEO) strings.mediaKindVideo else strings.mediaKindImage
                Text(
                    String.format(strings.mediaItemTitle, index + 1, kind),
                    color = ContentTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                String.format(strings.mediaItemDetails, item.mimeType ?: strings.mediaItemFallbackMime, formatBytes(item.contentLength, strings)),
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
    strings: AppStrings,
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
        contentPadding = PaddingValues(start = 26.dp, top = 8.dp, end = 26.dp, bottom = 112.dp),
    ) {
        item {
            RecordsPageHeader(
                title = title,
                subtitle = if (records.isEmpty()) subtitle else "$subtitle · ${records.size}",
            )
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
        itemsIndexed(records, key = { _, record -> record.managerId }) { index, record ->
            ContentFrame {
                RecordCard(
                    record = record,
                    showDivider = index != records.lastIndex,
                    strings = strings,
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
private fun RecordsPageHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 4.dp, bottom = 20.dp),
    ) {
        Text(
            text = title,
            color = ContentTextPrimary,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6f).sp,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = ContentTextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun RecordCard(
    record: DownloadRecord,
    showDivider: Boolean,
    strings: AppStrings,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Column {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        record.filename,
                        color = ContentTextPrimary,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${formatDate(record.updatedAt)} · ${record.platform.value} · ${statusLabel(record.status, strings)}",
                        color = ContentTextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            val progress = record.progress
            if (progress != null && record.isActive) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
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

            Spacer(Modifier.height(10.dp))

            Text(
                buildString {
                    append(formatBytes(record.bytesDownloaded, strings))
                    record.totalBytes?.let { append(" / ${formatBytes(it, strings)}") }
                    record.statusMessage?.let { append("  ·  $it") }
                },
                color = if (record.status == DownloadStatus.FAILED) MatteRose else ContentTextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    record.isActive -> HighRadiusButton(strings.btnCancel, icon = Icons.Default.Close, primary = false, onClick = onCancel)
                    record.status == DownloadStatus.SUCCEEDED -> {
                        HighRadiusButton(strings.btnOpenFile, icon = Icons.Default.PlayArrow, primary = true, onClick = onOpen)
                        HighRadiusButton(strings.btnShare, icon = Icons.Default.Share, primary = false, onClick = onShare)
                        HighRadiusButton(strings.btnDeleteFile, icon = Icons.Default.Delete, primary = false, onClick = onDelete)
                    }
                    else -> {
                        HighRadiusButton(strings.btnRetry, icon = Icons.Default.Refresh, primary = true, onClick = onRetry)
                        HighRadiusButton(strings.btnDeleteRecord, icon = Icons.Default.Delete, primary = false, onClick = onDelete)
                    }
                }
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ContentBorder.copy(alpha = 0.7f)),
            )
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
private fun CompactPageHeader(
    title: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = ContentTextPrimary,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
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

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN).format(Date(timestamp))

@Composable
private fun EmptyRecordsState(
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 48.dp),
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
private fun CompactStatusMessage(
    text: String,
    indicatorColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(indicatorColor),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = ContentTextMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
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

private fun statusLabel(status: DownloadStatus, strings: AppStrings): String = when (status) {
    DownloadStatus.QUEUED -> strings.statusQueued
    DownloadStatus.RUNNING -> strings.statusRunning
    DownloadStatus.PAUSED -> strings.statusPaused
    DownloadStatus.SUCCEEDED -> strings.statusSucceeded
    DownloadStatus.FAILED -> strings.statusFailed
    DownloadStatus.CANCELLED -> strings.statusCancelled
}

private fun estimatedSize(items: List<MediaItem>, strings: AppStrings): String =
    if (items.all { it.contentLength != null }) {
        formatBytes(items.sumOf { it.contentLength ?: 0L }, strings)
    } else {
        strings.downloadBytesUnknown
    }

private fun formatBytes(bytes: Long?, strings: AppStrings): String {
    bytes ?: return strings.downloadBytesUnknown
    return when {
        bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}
