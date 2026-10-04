package com.offipe.app.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OffipeColorScheme = darkColorScheme(
    primary = OffipeColors.Accent,
    onPrimary = OffipeColors.Black,
    primaryContainer = OffipeColors.AccentDim,
    onPrimaryContainer = OffipeColors.Black,
    secondary = OffipeColors.TextPrimary,
    onSecondary = OffipeColors.Black,
    background = OffipeColors.Black,
    onBackground = OffipeColors.TextPrimary,
    surface = OffipeColors.Surface,
    onSurface = OffipeColors.TextPrimary,
    surfaceVariant = OffipeColors.SurfaceHigh,
    onSurfaceVariant = OffipeColors.TextSecondary,
    error = OffipeColors.Danger,
    onError = OffipeColors.TextPrimary,
    outline = OffipeColors.Border,
    outlineVariant = OffipeColors.BorderStrong
)

/**
 * Single dark theme — Offipe doesn't have a light mode by design.
 * Pure monochrome: deep black backgrounds, layered charcoal surfaces,
 * high-contrast white text, minimal warm-white accent.
 */
@Composable
fun OffipeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = OffipeColors.Black.toArgb()
            window.navigationBarColor = OffipeColors.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = OffipeColorScheme,
        typography = OffipeTypography,
        shapes = OffipeShapes,
        content = content
    )
}
