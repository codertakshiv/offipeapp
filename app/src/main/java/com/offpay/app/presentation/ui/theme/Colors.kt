package com.offipe.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Offipe monochrome palette. Deep black canvas, layered charcoal surfaces,
 * high-contrast white text, minimal accent reserved for primary action.
 *
 * Use [Accent] sparingly — only for the single primary CTA on a screen.
 * Secondary actions use white-on-charcoal outlined buttons.
 */
object OffipeColors {
    // Surface
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0A0A0A)
    val SurfaceHigh = Color(0xFF141414)
    val SurfaceHigher = Color(0xFF1E1E1E)

    // Borders
    val Border = Color(0xFF2A2A2A)
    val BorderStrong = Color(0xFF3A3A3A)

    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0B0)
    val TextMuted = Color(0xFF707070)

    // Accent (Offipe signature — restrained warm white)
    val Accent = Color(0xFFF5F5F0)
    val AccentDim = Color(0xFFD0D0C8)
    val AccentDeep = Color(0xFF8A8A80)

    // Status
    val Danger = Color(0xFFFF4D4D)
    val DangerDim = Color(0xFFB23636)
    val DangerDeep = Color(0xFF6B1F1F)

    val Success = Color(0xFF00D26A)
    val SuccessDim = Color(0xFF008C47)
    val SuccessDeep = Color(0xFF00532B)

    val Warn = Color(0xFFFFB020)
    val WarnDim = Color(0xFFB37000)

    val Overlay = Color(0xCC000000)
}
