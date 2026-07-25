package com.jeremysu0818.igthreadsdl.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Dark Mode Palette (Background: #20201E)
val DarkMatteBg = Color(0xFF20201E)
val DarkMatteCard = Color(0xFF2A2A27)
val DarkMatteCardBorder = Color(0xFF383834)
val DarkMatteCardHover = Color(0xFF353531)

// Light Mode Palette (Background: #F9F9F7)
val LightMatteBg = Color(0xFFF9F9F7)
val LightMatteCard = Color(0xFFFFFFFF)
val LightMatteCardBorder = Color(0xFFE5E7EB)
val LightMatteCardHover = Color(0xFFEFEFEA)

// Accent Colors
val DefaultMattePrimary = Color(0xFFB86646)
val DefaultMatteEmerald = Color(0xFF396AB8)
val DefaultMatteAmber = Color(0xFFF59E0B)
val DefaultMatteRose = Color(0xFFEF4444)
val DefaultMatteRoseDark = Color(0xFF3D161A)
val DefaultMatteRoseLight = Color(0xFFFCA5A5)

val DarkTextPrimary = Color(0xFFF3F4F6)
val DarkTextSecondary = Color(0xFF9CA3AF)
val DarkTextMuted = Color(0xFF6B7280)

val LightTextPrimary = Color(0xFF1F2937)
val LightTextSecondary = Color(0xFF4B5563)
val LightTextMuted = Color(0xFF9CA3AF)

// Reference palette extracted with Python/Pillow from the four supplied screenshots.
// These tokens are intentionally separate from the existing Matte tokens because the
// bottom navigation bar must keep its original colors.
val ReferenceLightBackground = Color(0xFFF9F9F7)
val ReferenceLightSurface = Color(0xFFFFFFFF)
val ReferenceLightSubtleSurface = Color(0xFFF0EFEB)
val ReferenceLightBorder = Color(0xFFDDDDDB)
val ReferenceLightTextPrimary = Color(0xFF131313)
val ReferenceLightTextMuted = Color(0xFF7A7974)

val ReferenceDarkBackground = Color(0xFF20201E)
val ReferenceDarkSurface = Color(0xFF2C2C2A)
val ReferenceDarkSubtleSurface = Color(0xFF131313)
val ReferenceDarkBorder = Color(0xFF464644)
val ReferenceDarkTextPrimary = Color(0xFFF9F9F7)
val ReferenceDarkTextMuted = Color(0xFF98958E)

val ReferenceAccent = Color(0xFFCB7C5E)
val ReferenceLightSelection = Color(0xFF284E8F)
val ReferenceDarkSelection = Color(0xFF7AA6E7)
val ReferenceLightSwitchOn = Color(0xFF396AB9)
val ReferenceDarkSwitchOn = Color(0xFF6596E2)
val ReferenceLightSwitchOff = Color(0xFFE7E6E1)
val ReferenceDarkSwitchOff = Color(0xFF0B0B0B)

// View / Overlay ARGB Int Values
val MatteBgInt = DarkMatteBg.toArgb()
val MatteCardInt = DarkMatteCard.toArgb()
val MatteCardBorderInt = DarkMatteCardBorder.toArgb()
val MatteCardHoverInt = DarkMatteCardHover.toArgb()

val MattePrimaryInt = DefaultMattePrimary.toArgb()
val MatteEmeraldInt = DefaultMatteEmerald.toArgb()
val MatteAmberInt = DefaultMatteAmber.toArgb()
val MatteRoseInt = DefaultMatteRose.toArgb()
val MatteRoseDarkInt = DefaultMatteRoseDark.toArgb()
val MatteRoseLightInt = DefaultMatteRoseLight.toArgb()

val MatteTextPrimaryInt = DarkTextPrimary.toArgb()
val MatteTextSecondaryInt = DarkTextSecondary.toArgb()
val MatteTextMutedInt = DarkTextMuted.toArgb()
