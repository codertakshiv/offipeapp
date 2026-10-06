package com.offipe.app.presentation.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Offipe palette — premium minimalist offline-finance.
 *
 * The canvas is pure black; structure comes from near-black surfaces and
 * thin borders, not hue. Text is white → grey. Pastel green and pastel
 * red are STATUS ONLY: green fires exclusively on success / granted /
 * received states, red exclusively on failure / denied / error states.
 * Nothing decorative is ever tinted.
 */
object OffipeColors {
    // Canvas & surfaces — near-black stack
    val Canvas = Color(0xFF000000)
    val Black = Canvas
    val Surface = Color(0xFF111111)
    val SurfaceHigh = Color(0xFF171717)
    val SurfaceHigher = Color(0xFF1F1F1F)
    val SurfaceRaised = Color(0xFF262626)

    // Borders — thin, quiet
    val Border = Color(0xFF2A2A2A)
    val BorderStrong = Color(0xFF3D3D3D)

    // Text — white primary ramp
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9E9E9E)
    val TextMuted = Color(0xFF6E6E6E)

    // Accent — the white itself: primary buttons, active states, focus
    val Mark = Color(0xFFFFFFFF)
    val Accent = Mark
    val AccentDim = Color(0xFFE6E6E6)
    val Signal = Color(0xFFFFFFFF)

    // Grey ramp (was the logo wave bands) — secondary graphics only
    val WaveMid = Color(0xFF4A4A4A)
    val WaveSoft = Color(0xFF8E8E8E)
    val WaveLight = Color(0xFFD6D6D6)

    // Status — pastel green / pastel red, meaning only
    val Success = Color(0xFF7EE0A8)

    val Danger = Color(0xFFF4918C)

    val Warn = Color(0xFFE8C46A)

    val Overlay = Color(0xE6000000)
}
