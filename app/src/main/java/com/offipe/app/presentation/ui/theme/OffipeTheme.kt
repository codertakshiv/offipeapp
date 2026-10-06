package com.offipe.app.presentation.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OffipeColorScheme = darkColorScheme(
    primary = OffipeColors.Accent,
    onPrimary = OffipeColors.Black,
    primaryContainer = OffipeColors.SurfaceHigher,
    onPrimaryContainer = OffipeColors.TextPrimary,
    secondary = OffipeColors.Signal,
    onSecondary = OffipeColors.Black,
    background = OffipeColors.Black,
    onBackground = OffipeColors.TextPrimary,
    surface = OffipeColors.Surface,
    onSurface = OffipeColors.TextPrimary,
    surfaceVariant = OffipeColors.SurfaceHigh,
    onSurfaceVariant = OffipeColors.TextSecondary,
    error = OffipeColors.Danger,
    onError = OffipeColors.Black,
    outline = OffipeColors.Border,
    outlineVariant = OffipeColors.BorderStrong
)

/**
 * Single dark theme — Offipe has no light mode by design.
 * Pure-black canvas, near-black surfaces, thin borders, white accent;
 * pastel green / pastel red reserved for status. See [OffipeColors].
 */
@Composable
fun OffipeTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val activity = context as? Activity
            if (activity != null) {
                val window = activity.window
                @Suppress("DEPRECATION")
                window.statusBarColor = OffipeColors.Black.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = OffipeColors.Black.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = OffipeColorScheme,
        typography = OffipeTypography,
        shapes = OffipeShapes,
        content = content
    )
}
