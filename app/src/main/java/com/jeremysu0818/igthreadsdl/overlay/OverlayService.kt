package com.jeremysu0818.igthreadsdl.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.load
import com.jeremysu0818.igthreadsdl.AppGraph
import com.jeremysu0818.igthreadsdl.MainActivity
import com.jeremysu0818.igthreadsdl.R
import com.jeremysu0818.igthreadsdl.data.resolver.UrlNormalizer
import com.jeremysu0818.igthreadsdl.domain.model.MediaItem
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.permissions.PermissionStatus
import com.jeremysu0818.igthreadsdl.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

import com.jeremysu0818.igthreadsdl.i18n.AppStrings
import com.jeremysu0818.igthreadsdl.i18n.LanguageManager

private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun init() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

private class CloseTargetState {
    var isVisible by mutableStateOf(false)
    var isActive by mutableStateOf(false)

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
        isActive = false
    }
}

private val CaptionCloseTargetDarkColorScheme = darkColorScheme(
    primary = ComposeColor(0xFFD0BCFF),
    secondary = ComposeColor(0xFFCCC2DC),
    tertiary = ComposeColor(0xFFEFB8C8),
)

private val CaptionCloseTargetLightColorScheme = lightColorScheme(
    primary = ComposeColor(0xFF6650A4),
    secondary = ComposeColor(0xFF625B71),
    tertiary = ComposeColor(0xFF7D5260),
)

@Composable
private fun CaptionCloseTargetTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        CaptionCloseTargetDarkColorScheme
    } else {
        CaptionCloseTargetLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

@Composable
private fun CloseTargetApp(state: CloseTargetState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val savedLang = LanguageManager.getSavedLanguage(context)
    val strings = LanguageManager.getStrings(savedLang)
    val resolvedLang = LanguageManager.resolveAppLanguage(savedLang)
    val layoutDirection = if (resolvedLang == com.jeremysu0818.igthreadsdl.i18n.AppLanguage.AR) {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    } else {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = state.isVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Box(modifier = Modifier.padding(bottom = 20.dp)) {
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .width(128.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isActive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (state.isActive) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) {
                        Text(text = strings.overlayDragToStop)
                    }
                }
            }
        }
    }
}

private fun isPointInsideCapsule(
    x: Float,
    y: Float,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
): Boolean {
    val radius = height / 2f
    val centerY = top + radius
    val leftCircleCenterX = left + radius
    val rightCircleCenterX = left + width - radius

    return when {
        x in leftCircleCenterX..rightCircleCenterX && y in top..(top + height) -> true
        x < leftCircleCenterX -> (x - leftCircleCenterX) * (x - leftCircleCenterX) +
            (y - centerY) * (y - centerY) <= radius * radius
        x > rightCircleCenterX -> (x - rightCircleCenterX) * (x - rightCircleCenterX) +
            (y - centerY) * (y - centerY) <= radius * radius
        else -> false
    }
}

internal fun shouldCloseAfterDrag(actionMasked: Int, isOverCloseTarget: Boolean): Boolean =
    actionMasked == MotionEvent.ACTION_UP && isOverCloseTarget

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: AccessibleBubbleView
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val selectedIds = linkedSetOf<String>()
    private var lastManifestKey: String? = null
    private var currentState = OverlayState()
    private var panelControls = false
    private var panelHint: String? = null
    private var clipboardReadJob: Job? = null
    private var bubbleOnRight = true
    private var lastOutsideDismissAt = 0L
    private var panelUrlText = ""
    private var panelUrlInput: EditText? = null
    private var closeTargetView: ComposeView? = null
    private var closeTargetLifecycle: OverlayLifecycleOwner? = null
    private val closeTargetState = CloseTargetState()

    private val strings: AppStrings
        get() = LanguageManager.getStrings(LanguageManager.getSavedLanguage(this))

    private val languageSettingsListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == LanguageManager.KEY_LANGUAGE) {
                createNotificationChannel()
                updateNotification()
                if (panelView != null) renderPanel()
            }
        }

    override fun onCreate() {
        super.onCreate()
        getSharedPreferences(PermissionStatus.OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putBoolean(PermissionStatus.KEY_OVERLAY_SERVICE_ACTIVE, true)
            }
        getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(languageSettingsListener)
        createNotificationChannel()
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createBubble()
        serviceScope.launch {
            AppGraph.overlayCoordinator.state.collectLatest { state ->
                val downloadJustCompleted =
                    currentState.phase == OverlayPhase.DOWNLOADING &&
                        state.phase == OverlayPhase.READY &&
                        state.completedSelectionIds.isNotEmpty()
                currentState = state
                val manifestKey = state.manifest?.sourceUrl
                if (manifestKey != null && manifestKey != lastManifestKey) {
                    lastManifestKey = manifestKey
                    selectedIds.clear()
                    selectedIds += state.manifest.items.map { it.id }
                }
                renderBubble(state)
                if (panelView != null) renderPanel()
                if (downloadJustCompleted) {
                    Toast.makeText(
                        this@OverlayService,
                        strings.downloadStatusSucceeded,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            closeOverlay()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!::windowManager.isInitialized || !::bubbleParams.isInitialized) return

        bubbleParams.x = if (bubbleOnRight) {
            resources.displayMetrics.widthPixels - dp(BUBBLE_EDGE_OFFSET_DP)
        } else {
            dp(BUBBLE_MARGIN_DP)
        }
        clampBubbleToScreen()
        runCatching { windowManager.updateViewLayout(bubbleView, bubbleParams) }
        saveBubblePosition()
        updatePanelPosition()
    }

    override fun onDestroy() {
        getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(languageSettingsListener)
        getSharedPreferences(PermissionStatus.OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putBoolean(PermissionStatus.KEY_OVERLAY_SERVICE_ACTIVE, false)
            }
        serviceScope.cancel()
        removePanel()
        removeCloseTargetView()
        if (::windowManager.isInitialized && ::bubbleView.isInitialized) {
            runCatching { windowManager.removeView(bubbleView) }
        }
        super.onDestroy()
    }

    private fun createBubble() {
        val preferences = getSharedPreferences(
            PermissionStatus.OVERLAY_PREFERENCES,
            Context.MODE_PRIVATE,
        )
        bubbleView = AccessibleBubbleView(this).apply {
            gravity = Gravity.CENTER
            textSize = BUBBLE_TEXT_SIZE_SP
            setTextColor(MatteTextPrimaryInt)
            elevation = 12f
            contentDescription = strings.overlayAccessibilityDescription
            setPadding(0, 0, 0, dp(1))
        }
        val savedX = preferences.getInt(KEY_X, resources.displayMetrics.widthPixels - dp(BUBBLE_EDGE_OFFSET_DP))
        bubbleOnRight = preferences.getBoolean(KEY_EDGE_RIGHT, savedX > dp(BUBBLE_EDGE_OFFSET_DP))
        bubbleParams = WindowManager.LayoutParams(
            dp(BUBBLE_SIZE_DP),
            dp(BUBBLE_SIZE_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (bubbleOnRight) {
                resources.displayMetrics.widthPixels - dp(BUBBLE_EDGE_OFFSET_DP)
            } else {
                dp(BUBBLE_MARGIN_DP)
            }
            y = preferences.getInt(KEY_Y, resources.displayMetrics.heightPixels / 3)
        }
        clampBubbleToScreen()
        attachBubbleGestures()
        runCatching { windowManager.addView(bubbleView, bubbleParams) }
            .onSuccess { createCloseTargetView() }
            .onFailure { stopSelf() }
    }

    private fun createCloseTargetView() {
        if (closeTargetView != null) return

        val density = resources.displayMetrics.density
        val closeTargetHeightPx = (112 * density).roundToInt()
        val closeTargetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            closeTargetHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            alpha = 1f
        }

        val owner = OverlayLifecycleOwner().apply { init() }
        val cView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                CaptionCloseTargetTheme {
                    CloseTargetApp(closeTargetState)
                }
            }
        }
        runCatching { windowManager.addView(cView, closeTargetParams) }
        closeTargetView = cView
        closeTargetLifecycle = owner
    }

    private fun removeCloseTargetView() {
        closeTargetState.hide()
        closeTargetView?.let { view ->
            if (::windowManager.isInitialized) {
                runCatching { windowManager.removeView(view) }
            }
        }
        closeTargetView = null
        closeTargetLifecycle?.destroy()
        closeTargetLifecycle = null
    }

    private fun attachBubbleGestures() {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        var longPressed = false
        var longPressJob: Job? = null

        val density = resources.displayMetrics.density
        val closeCapsuleWidthPx = (128 * density).roundToInt()
        val closeCapsuleHeightPx = (48 * density).roundToInt()
        val closeCapsuleBottomMarginPx = (20 * density).roundToInt()

        bubbleView.setOnClickListener { beginBubbleAction() }
        bubbleView.setOnLongClickListener {
            panelControls = true
            panelHint = null
            showPanel()
            true
        }
        bubbleView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = bubbleParams.x
                    startY = bubbleParams.y
                    moved = false
                    longPressed = false
                    longPressJob?.cancel()
                    closeTargetState.show()
                    longPressJob = serviceScope.launch {
                        kotlinx.coroutines.delay(ViewConfiguration.getLongPressTimeout().toLong())
                        if (!moved) {
                            longPressed = bubbleView.performLongClick()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        moved = true
                        longPressJob?.cancel()
                    }
                    if (moved) {
                        bubbleParams.x = (startX + dx).roundToInt().coerceIn(
                            0,
                            resources.displayMetrics.widthPixels - dp(BUBBLE_SIZE_DP),
                        )
                        bubbleParams.y = (startY + dy).roundToInt().coerceIn(
                            dp(24),
                            resources.displayMetrics.heightPixels - dp(80),
                        )
                        runCatching {
                            windowManager.updateViewLayout(bubbleView, bubbleParams)
                            updatePanelPosition()
                        }

                        val screenWidthPixels = resources.displayMetrics.widthPixels
                        val screenHeightPixels = resources.displayMetrics.heightPixels
                        val capsuleLeft = (screenWidthPixels - closeCapsuleWidthPx) / 2f
                        val capsuleTop = (
                            screenHeightPixels - closeCapsuleBottomMarginPx - closeCapsuleHeightPx
                        ).toFloat()
                        val isOverCloseTarget = isPointInsideCapsule(
                            x = event.rawX,
                            y = event.rawY,
                            left = capsuleLeft,
                            top = capsuleTop,
                            width = closeCapsuleWidthPx.toFloat(),
                            height = closeCapsuleHeightPx.toFloat(),
                        )
                        if (isOverCloseTarget && !closeTargetState.isActive) {
                            closeTargetView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        closeTargetState.isActive = isOverCloseTarget
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressJob?.cancel()
                    val shouldClose = shouldCloseAfterDrag(
                        actionMasked = event.actionMasked,
                        isOverCloseTarget = closeTargetState.isActive,
                    )
                    closeTargetState.hide()
                    if (shouldClose) {
                        closeOverlay()
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        if (!moved && !longPressed) {
                            view.performClick()
                        } else if (moved) {
                            snapBubbleToEdge()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun beginBubbleAction() {
        clipboardReadJob?.cancel()
        if (
            SystemClock.elapsedRealtime() - lastOutsideDismissAt <=
            OUTSIDE_DISMISS_DEDUPLICATION_MS
        ) {
            lastOutsideDismissAt = 0L
            return
        }
        if (panelView != null) {
            removePanel()
            return
        }
        if (
            currentState.phase == OverlayPhase.RESOLVING ||
            currentState.phase == OverlayPhase.DOWNLOADING
        ) {
            showPanel()
            return
        }
        setBubbleFocusable(true)
        clipboardReadJob = serviceScope.launch {
            kotlinx.coroutines.delay(CLIPBOARD_FOCUS_DELAY_MS)
            try {
                handleBubbleClick()
            } finally {
                setBubbleFocusable(false)
            }
        }
    }

    private fun setBubbleFocusable(focusable: Boolean) {
        if (!::windowManager.isInitialized || !::bubbleParams.isInitialized) return
        bubbleParams.flags = if (focusable) {
            bubbleParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            bubbleParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        runCatching { windowManager.updateViewLayout(bubbleView, bubbleParams) }
    }

    private fun handleBubbleClick() {
        panelControls = false
        panelHint = null
        val clipboardUrl = readClipboardUrl()
        if (clipboardUrl != null) panelUrlText = clipboardUrl
        when (
            bubbleContentAction(
                clipboardUrl = clipboardUrl,
                detectedUrl = currentState.detectedUrl,
                hasManifest = currentState.manifest != null,
            )
        ) {
            BubbleContentAction.SHOW_CURRENT -> showPanel()
            BubbleContentAction.SHOW_CLIPBOARD_HINT -> {
                panelHint = strings.overlayClipboardInvalidHint
                showPanel()
            }
            BubbleContentAction.RESOLVE_CLIPBOARD -> {
                showPanel()
                AppGraph.overlayCoordinator.resolve(checkNotNull(clipboardUrl))
            }
        }
    }

    private fun showPanel() {
        if (panelUrlText.isBlank()) {
            panelUrlText = currentState.detectedUrl.orEmpty()
        }
        if (panelView == null) {
            panelParams = WindowManager.LayoutParams(
                panelWidth(),
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            panelView = OutsideDismissFrameLayout(this) { event ->
                lastOutsideDismissAt = if (isTouchInsideBubble(event)) {
                    SystemClock.elapsedRealtime()
                } else {
                    0L
                }
                removePanel()
            }
            updatePanelPosition()
            runCatching { windowManager.addView(panelView, panelParams) }
                .onFailure {
                    panelView = null
                    panelParams = null
                }
        }
        renderPanel()
    }

    private fun renderPanel() {
        val frame = panelView as? FrameLayout ?: return
        frame.removeAllViews()

        val resolvedLang = LanguageManager.resolveAppLanguage(LanguageManager.getSavedLanguage(this))
        val isRtl = resolvedLang == com.jeremysu0818.igthreadsdl.i18n.AppLanguage.AR
        val dir = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        val textDir = if (isRtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR

        frame.layoutDirection = dir
        frame.textDirection = textDir

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedDrawable(COLOR_PANEL, dp(22).toFloat())
            elevation = 16f
            layoutDirection = dir
            textDirection = textDir
        }
        frame.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        content.addView(buildHeader())

        if (panelControls) {
            renderControlMenu(content)
            return
        }
        content.addView(buildUrlInput().withTopMargin(10))
        panelHint?.let {
            content.addView(bodyText(it, COLOR_MUTED).withTopMargin(12))
            return
        }

        when (currentState.phase) {
            OverlayPhase.IDLE -> {
                content.addView(
                    bodyText(
                        strings.overlayIdleHint,
                        COLOR_MUTED,
                    ).withTopMargin(12),
                )
            }
            OverlayPhase.RESOLVING -> {
                content.addView(bodyText(strings.overlayResolvingText, COLOR_ACCENT).withTopMargin(12))
            }
            OverlayPhase.READY, OverlayPhase.DOWNLOADING -> renderManifest(content)
            OverlayPhase.ERROR -> {
                content.addView(
                    bodyText(
                        currentState.errorMessage ?: strings.overlayErrorDefault,
                        COLOR_ERROR,
                    ).withTopMargin(12),
                )
                currentState.detectedUrl?.let {
                    content.addView(
                        primaryButton(strings.overlayBtnRetryParse) {
                            AppGraph.overlayCoordinator.resolve(it)
                        }.withTopMargin(14),
                    )
                }
                if (currentState.manifest != null) {
                    content.addView(
                        secondaryButton(strings.overlayBtnBackPreview) {
                            AppGraph.overlayCoordinator.clearError()
                        }.withTopMargin(8),
                    )
                }
            }
        }
    }

    private fun buildHeader(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val handleBar = View(this).apply {
            background = roundedDrawable(
                Color.argb(76, 255, 255, 255),
                2.5f * resources.displayMetrics.density,
            )
        }
        container.addView(
            handleBar,
            LinearLayout.LayoutParams(dp(36), dp(5)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(6)
            },
        )
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            TextView(this).apply {
                text = getString(R.string.overlay_brand)
                textSize = 13f
                letterSpacing = 0.12f
                setTextColor(MatteTextPrimaryInt)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        row.addView(iconButton("—", strings.overlayBtnMinimize) { removePanel() })
        row.addView(iconButton("×", strings.overlayBtnClose) { closeOverlay() })
        container.addView(row)
        attachPanelDragGestures(container)
        return container
    }

    private fun attachPanelDragGestures(dragHandle: View) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        dragHandle.setOnTouchListener { _, event ->
            val params = panelParams ?: return@setOnTouchListener false
            val pView = panelView ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    val location = IntArray(2)
                    pView.getLocationOnScreen(location)
                    startX = location[0]
                    startY = location[1]
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        moved = true
                    }
                    if (moved) {
                        val screenWidth = resources.displayMetrics.widthPixels
                        val screenHeight = resources.displayMetrics.heightPixels
                        val panelWidth = pView.width.takeIf { it > 0 } ?: params.width
                        val panelHeight = pView.height.takeIf { it > 0 } ?: dp(300)
                        params.x = (startX + dx).roundToInt().coerceIn(
                            0,
                            (screenWidth - panelWidth).coerceAtLeast(0),
                        )
                        params.y = (startY + dy).roundToInt().coerceIn(
                            0,
                            (screenHeight - panelHeight).coerceAtLeast(0),
                        )
                        runCatching { windowManager.updateViewLayout(pView, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun buildUrlInput(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(
            TextView(this).apply {
                text = strings.overlayPostUrlLabel
                textSize = 11f
                setTextColor(COLOR_MUTED)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            },
        )
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val input = EditText(this).apply {
            setText(panelUrlText)
            setSelection(text.length)
            hint = strings.overlayInputHint
            setHintTextColor(COLOR_MUTED)
            setTextColor(MatteTextPrimaryInt)
            textSize = 12f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            setPadding(dp(10), 0, dp(10), 0)
            background = roundedDrawable(COLOR_PANEL_ALT, dp(10).toFloat())
            addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        text: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int,
                    ) = Unit

                    override fun onTextChanged(
                        text: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int,
                    ) {
                        panelUrlText = text?.toString().orEmpty()
                    }

                    override fun afterTextChanged(text: Editable?) = Unit
                },
            )
            setOnEditorActionListener { _, actionId, event ->
                val submitted = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                    event?.keyCode == KeyEvent.KEYCODE_ENTER
                if (submitted) submitPanelUrl()
                submitted
            }
        }
        panelUrlInput = input
        row.addView(
            input,
            LinearLayout.LayoutParams(
                0,
                dp(42),
                1f,
            ),
        )
        row.addView(
            primaryButton(
                text = if (currentState.phase == OverlayPhase.RESOLVING) strings.overlayBtnParsing else strings.overlayBtnParse,
                enabled = currentState.phase != OverlayPhase.RESOLVING &&
                    currentState.phase != OverlayPhase.DOWNLOADING,
                action = ::submitPanelUrl,
            ),
            LinearLayout.LayoutParams(
                dp(68),
                dp(42),
            ).apply { marginStart = dp(8) },
        )
        container.addView(
            row,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(5) },
        )
        return container
    }

    private fun submitPanelUrl() {
        if (
            currentState.phase == OverlayPhase.RESOLVING ||
            currentState.phase == OverlayPhase.DOWNLOADING
        ) {
            return
        }
        val supportedUrl = UrlNormalizer.extractSupportedUrl(panelUrlText)
        if (supportedUrl == null) {
            panelUrlInput?.apply {
                error = strings.overlayInputErrorInvalid
                requestFocus()
            }
            return
        }

        panelUrlText = supportedUrl
        panelHint = null
        panelUrlInput?.clearFocus()
        AppGraph.overlayCoordinator.resolve(supportedUrl)
    }

    private fun renderControlMenu(content: LinearLayout) {
        content.addView(
            bodyText(
                strings.overlayControlTitle,
                COLOR_MUTED,
            ).withTopMargin(12),
        )
        content.addView(
            secondaryButton(strings.overlayBtnResetPosition) {
                moveToDefaultPosition()
                removePanel()
            }.withTopMargin(14),
        )
        content.addView(
            dangerButton(strings.overlayBtnClose) { closeOverlay() }.withTopMargin(8),
        )
    }

    private fun renderManifest(content: LinearLayout) {
        val manifest = currentState.manifest ?: return
        val source = manifest.author?.let { "@$it" } ?: manifest.platform.value
        content.addView(
            bodyText(
                "${manifest.platform.value.uppercase()}  ·  $source  ·  ${manifest.type.value}",
                COLOR_MUTED,
            ).withTopMargin(10),
        )
        manifest.thumbnailUrl?.let { thumbnailUrl ->
            content.addView(
                ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = roundedDrawable(COLOR_PANEL_ALT, dp(12).toFloat())
                    clipToOutline = true
                    load(thumbnailUrl) {
                        crossfade(true)
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(118),
                ).apply { topMargin = dp(10) },
            )
        }
        content.addView(
            bodyText(
                String.format(strings.overlayItemsSummary, manifest.items.size, estimatedSize(manifest.items)),
                MatteTextPrimaryInt,
            ).withTopMargin(10),
        )
        if (manifest.isPartial) {
            content.addView(
                bodyText(strings.msgPartialManifest, COLOR_WARNING).withTopMargin(8),
            )
        }

        val itemList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        manifest.items.forEachIndexed { index, item ->
            itemList.addView(mediaCheckbox(item, index))
        }
        content.addView(
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                addView(itemList)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(154).coerceAtMost(dp(48) * manifest.items.size + dp(8)),
            ).apply { topMargin = dp(8) },
        )
        val selectionAlreadyDownloaded =
            selectedIds.isNotEmpty() &&
                selectedIds == currentState.completedSelectionIds
        if (currentState.completedSelectionIds.isNotEmpty()) {
            content.addView(
                bodyText(strings.downloadStatusSucceeded, COLOR_READY).withTopMargin(10),
            )
        }
        val downloadLabel = when {
            currentState.phase == OverlayPhase.DOWNLOADING -> strings.overlayBtnDownloading
            selectionAlreadyDownloaded -> strings.downloadStatusSucceeded
            else -> strings.overlayBtnDownloadSelected
        }
        content.addView(
            primaryButton(
                text = downloadLabel,
                enabled = currentState.phase != OverlayPhase.DOWNLOADING &&
                    !selectionAlreadyDownloaded,
            ) {
                AppGraph.overlayCoordinator.download(selectedIds)
            }.withTopMargin(12),
        )
        content.addView(
            secondaryButton(strings.overlayBtnOpenInApp) {
                openInApp(manifest.sourceUrl)
            }.withTopMargin(8),
        )
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(strings.overlayNotificationTitle)
            .setContentText(strings.overlayNotificationText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, strings.overlayNotificationActionStop, stopIntent)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun estimatedSize(items: List<MediaItem>): String {
        val sizes = items.map { it.contentLength }
        return if (sizes.all { it != null }) {
            formatBytes(sizes.filterNotNull().sum())
        } else {
            strings.downloadBytesUnknown
        }
    }

    private fun formatBytes(bytes: Long?): String {
        bytes ?: return strings.downloadBytesUnknown
        return when {
            bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024 -> String.format(Locale.US, "%.1f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }

    private fun mediaCheckbox(item: MediaItem, index: Int): View =
        CheckBox(this).apply {
            val kind = if (item.type == MediaItemType.VIDEO) {
                getString(R.string.media_kind_video)
            } else {
                getString(R.string.media_kind_image)
            }
            text = getString(
                R.string.overlay_media_item,
                index + 1,
                kind,
                formatBytes(item.contentLength),
            )
            textSize = 13f
            setTextColor(MatteTextPrimaryInt)
            buttonTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(),
                ),
                intArrayOf(COLOR_ACCENT, COLOR_MUTED),
            )
            isChecked = item.id in selectedIds
            setOnCheckedChangeListener { _, checked ->
                if (checked) selectedIds += item.id else selectedIds -= item.id
                panelView?.post { renderPanel() }
            }
        }

    private fun renderBubble(state: OverlayState) {
        if (!::bubbleView.isInitialized) return
        val (label, color) = when {
            state.phase == OverlayPhase.IDLE -> "↓" to COLOR_IDLE
            state.phase == OverlayPhase.RESOLVING -> "…" to COLOR_ACCENT
            state.phase == OverlayPhase.READY -> "✓" to COLOR_READY
            state.phase == OverlayPhase.DOWNLOADING -> "⇩" to COLOR_ACCENT
            else -> "!" to COLOR_ERROR
        }
        bubbleView.text = label
        bubbleView.background = roundedDrawable(color, dp(BUBBLE_RADIUS_DP))
        bubbleView.alpha = if (state.phase == OverlayPhase.IDLE) 0.5f else 0.7f
    }

    private fun snapBubbleToEdge() {
        val width = resources.displayMetrics.widthPixels
        bubbleOnRight = bubbleParams.x + dp(BUBBLE_RADIUS_DP).roundToInt() >= width / 2
        bubbleParams.x = if (!bubbleOnRight) {
            dp(BUBBLE_MARGIN_DP)
        } else {
            width - dp(BUBBLE_EDGE_OFFSET_DP)
        }
        runCatching { windowManager.updateViewLayout(bubbleView, bubbleParams) }
        saveBubblePosition()
        updatePanelPosition()
    }

    private fun moveToDefaultPosition() {
        bubbleOnRight = true
        bubbleParams.x = resources.displayMetrics.widthPixels - dp(BUBBLE_EDGE_OFFSET_DP)
        bubbleParams.y = resources.displayMetrics.heightPixels / 3
        runCatching { windowManager.updateViewLayout(bubbleView, bubbleParams) }
        saveBubblePosition()
        updatePanelPosition()
    }

    private fun saveBubblePosition() {
        getSharedPreferences(PermissionStatus.OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putInt(KEY_X, bubbleParams.x)
                putInt(KEY_Y, bubbleParams.y)
                putBoolean(KEY_EDGE_RIGHT, bubbleOnRight)
            }
    }

    private fun updatePanelPosition() {
        val params = panelParams ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val panelWidth = panelWidth()
        params.width = panelWidth
        params.x = if (bubbleParams.x > screenWidth / 2) {
            (bubbleParams.x - panelWidth - dp(BUBBLE_MARGIN_DP)).coerceAtLeast(dp(BUBBLE_MARGIN_DP))
        } else {
            (bubbleParams.x + dp(BUBBLE_EDGE_OFFSET_DP)).coerceAtMost(screenWidth - panelWidth - dp(BUBBLE_MARGIN_DP))
        }
        val panelHeight = panelView?.height ?: 0
        val maxY = if (panelHeight > 0) {
            (screenHeight - panelHeight).coerceAtLeast(0)
        } else {
            (screenHeight - dp(240)).coerceAtLeast(0)
        }
        params.y = bubbleParams.y.coerceIn(0, maxY)
        panelView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
    }

    private fun panelWidth(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val availableBesideBubble = if (bubbleParams.x > screenWidth / 2) {
            bubbleParams.x - dp(16)
        } else {
            screenWidth - (bubbleParams.x + dp(BUBBLE_SIZE_DP)) - dp(16)
        }
        return dp(336).coerceAtMost(availableBesideBubble.coerceAtLeast(dp(1)))
    }

    private fun clampBubbleToScreen() {
        val maxX = (resources.displayMetrics.widthPixels - dp(BUBBLE_SIZE_DP)).coerceAtLeast(0)
        val minY = 0
        val maxY = (resources.displayMetrics.heightPixels - dp(80)).coerceAtLeast(minY)
        bubbleParams.x = bubbleParams.x.coerceIn(0, maxX)
        bubbleParams.y = bubbleParams.y.coerceIn(minY, maxY)
    }

    private fun isTouchInsideBubble(event: MotionEvent): Boolean {
        if (!::bubbleView.isInitialized) return false
        val location = IntArray(2)
        return runCatching {
            bubbleView.getLocationOnScreen(location)
            event.rawX >= location[0] &&
                event.rawX <= location[0] + bubbleView.width &&
                event.rawY >= location[1] &&
                event.rawY <= location[1] + bubbleView.height
        }.getOrDefault(false)
    }

    private fun removePanel() {
        panelView?.let { runCatching { windowManager.removeView(it) } }
        panelView = null
        panelParams = null
        panelUrlInput = null
        panelControls = false
        panelHint = null
    }

    private fun closeOverlay() {
        getSharedPreferences(PermissionStatus.OVERLAY_PREFERENCES, Context.MODE_PRIVATE)
            .edit {
                putBoolean(PermissionStatus.KEY_OVERLAY_ENABLED, false)
            }
        stopSelf()
    }

    private fun readClipboardUrl(): String? {
        return runCatching {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(this)
                ?.toString()
                .orEmpty()
            UrlNormalizer.extractSupportedUrl(text)
        }.getOrNull()
    }

    private fun openInApp(sourceUrl: String?) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_SOURCE_URL, sourceUrl),
        )
        removePanel()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                strings.overlayNotificationTitle,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = strings.overlayNotificationText
                setShowBadge(false)
            },
        )
    }

    private fun startAsForeground() {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_download)
            .setContentTitle(strings.overlayNotificationTitle)
            .setContentText(strings.overlayNotificationText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, strings.overlayNotificationActionStop, stopIntent)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun bodyText(text: String, color: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(color)
        setLineSpacing(0f, 1.15f)
    }

    private fun primaryButton(
        text: String,
        enabled: Boolean = true,
        action: () -> Unit,
    ): TextView = actionButton(text, if (enabled) COLOR_ACCENT else COLOR_IDLE, MatteTextPrimaryInt) {
        if (enabled) action()
    }.apply { isEnabled = enabled }

    private fun secondaryButton(text: String, action: () -> Unit): TextView =
        actionButton(text, COLOR_PANEL_ALT, MatteTextPrimaryInt, action)

    private fun dangerButton(text: String, action: () -> Unit): TextView =
        actionButton(text, COLOR_ERROR_DARK, COLOR_ERROR_LIGHT, action)

    private fun actionButton(
        text: String,
        backgroundColor: Int,
        textColor: Int,
        action: () -> Unit,
    ): TextView = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        textSize = 14f
        setTextColor(textColor)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setPadding(dp(12), dp(11), dp(12), dp(11))
        background = roundedDrawable(backgroundColor, dp(12).toFloat())
        setOnClickListener { action() }
    }

    private fun iconButton(text: String, description: String, action: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            contentDescription = description
            gravity = Gravity.CENTER
            textSize = 22f
            setTextColor(COLOR_MUTED)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
        }

    private fun View.withTopMargin(margin: Int): View = apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(margin) }
    }

    private fun roundedDrawable(color: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    companion object {
        private const val BUBBLE_SCALE = 0.7f
        private const val BASE_BUBBLE_SIZE_DP = 56
        private val BUBBLE_SIZE_DP = (BASE_BUBBLE_SIZE_DP * BUBBLE_SCALE).roundToInt() // 39 dp
        private val BUBBLE_RADIUS_DP = BUBBLE_SIZE_DP / 2f // 19.5 dp
        private const val BUBBLE_MARGIN_DP = 8
        private val BUBBLE_EDGE_OFFSET_DP = BUBBLE_SIZE_DP + BUBBLE_MARGIN_DP // 47 dp
        private val BUBBLE_TEXT_SIZE_SP = 22f * BUBBLE_SCALE // 15.4f

        private const val NOTIFICATION_CHANNEL_ID = "overlay_service"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP =
            "com.jeremysu0818.igthreadsdl.action.STOP_OVERLAY"
        private const val KEY_X = "bubble_x"
        private const val KEY_Y = "bubble_y"
        private const val KEY_EDGE_RIGHT = "bubble_edge_right"
        private const val CLIPBOARD_FOCUS_DELAY_MS = 80L
        private const val OUTSIDE_DISMISS_DEDUPLICATION_MS = 500L

        private val COLOR_PANEL = MatteCardInt
        private val COLOR_PANEL_ALT = MatteCardHoverInt
        private val COLOR_IDLE = MatteCardBorderInt
        private val COLOR_ACCENT = MattePrimaryInt
        private val COLOR_READY = MatteEmeraldInt
        private val COLOR_ERROR = MatteRoseInt
        private val COLOR_ERROR_DARK = MatteRoseDarkInt
        private val COLOR_ERROR_LIGHT = MatteRoseLightInt
        private val COLOR_WARNING = MatteAmberInt
        private val COLOR_MUTED = MatteTextSecondaryInt
    }
}

internal enum class BubbleContentAction {
    SHOW_CURRENT,
    SHOW_CLIPBOARD_HINT,
    RESOLVE_CLIPBOARD,
}

internal fun bubbleContentAction(
    clipboardUrl: String?,
    detectedUrl: String?,
    hasManifest: Boolean,
): BubbleContentAction = when {
    clipboardUrl == null && hasManifest -> BubbleContentAction.SHOW_CURRENT
    clipboardUrl == null -> BubbleContentAction.SHOW_CLIPBOARD_HINT
    clipboardUrl == detectedUrl && hasManifest -> BubbleContentAction.SHOW_CURRENT
    else -> BubbleContentAction.RESOLVE_CLIPBOARD
}

private class AccessibleBubbleView(context: Context) : TextView(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

private class OutsideDismissFrameLayout(
    context: Context,
    private val dismiss: (MotionEvent) -> Unit,
) : FrameLayout(context) {
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            dismiss(event)
            return true
        }
        return super.onTouchEvent(event)
    }
}
