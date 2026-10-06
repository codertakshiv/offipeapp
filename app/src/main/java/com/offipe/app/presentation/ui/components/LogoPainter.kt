package com.offipe.app.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.offipe.app.R

/**
 * Shared process-wide painter for the Offipe mark.
 *
 * `painterResource` decodes the PNG once per CALL SITE, so Splash, Home and
 * Settings each held their own decoded bitmap (≈590 kB apiece) and each paid
 * the decode cost on first composition. One cached painter serves all three.
 *
 * Written only from composition, which is main-thread — safe without a lock.
 */
@Volatile
private var logoPainter: Painter? = null

@Composable
fun offipeLogoPainter(): Painter {
    logoPainter?.let { return it }
    return painterResource(R.drawable.offipe_logo).also { logoPainter = it }
}
