package com.offipe.app.presentation.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.offipe.app.data.PreferencesRepository
import com.offipe.app.data.UserNameRules
import com.offipe.app.presentation.permissions.PermissionStatus
import com.offipe.app.presentation.permissions.openAccessibilitySettings
import com.offipe.app.presentation.permissions.openOverlaySettings
import com.offipe.app.presentation.permissions.rememberPermissionLaunchers
import com.offipe.app.presentation.permissions.rememberPermissionStatus
import com.offipe.app.presentation.ui.components.AlertStrip
import com.offipe.app.presentation.ui.components.AlertTone
import com.offipe.app.presentation.ui.components.GhostActionBar
import com.offipe.app.presentation.ui.components.Hairline
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.InlineField
import com.offipe.app.presentation.ui.components.LedDot
import com.offipe.app.presentation.ui.components.OffipeDialogSurface
import com.offipe.app.presentation.ui.components.ReadoutRow
import com.offipe.app.presentation.ui.components.Tag
import com.offipe.app.presentation.ui.components.TextKey
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect

private const val GITHUB_REPO_URL = "https://github.com/codertakshiv/offipeapp/"

/**
 * ONBOARDING — six steps in the reference's hero voice.
 *
 * Top strip: back key, passive dot progress, step readout. Navigation is
 * arrow-only: ONE circular right-arrow key docked bottom-end advances every
 * step (no tabs, no swipe, dots are never a control).
 *
 *  1. "Pay Offline."              — premium tilted phone + `*99#` screen.
 *  2. "Your Money. Your Control." — what `*99#` is + carrier support table.
 *  3. "Fast. Simple. Reliable."   — same phone locked onto a QR card.
 *  4. What should we call you?    — optional name, persisted once.
 *  5. Permissions                 — "Step 5 of 6", Grant/Granted rows.
 *  6. Ready                       — "You're set" (arrow completes setup).
 *
 * The saved name flows back into Home's time-based greeting. Blank input is
 * allowed and simply yields the nameless greeting.
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    prefsRepo: PreferencesRepository? = null
) {
    val totalPages = 6
    var page by rememberSaveable { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val permissionsState = rememberPermissionStatus()
    val permissions = permissionsState.value

    // Draft only lives until the user steps past it — then it is persisted.
    var nameDraft by rememberSaveable { mutableStateOf("") }

    val persistName = {
        scope.launch { prefsRepo?.setUserName(UserNameRules.sanitize(nameDraft)) }
    }

    fun next() {
        if (page == NAME_STEP) persistName()
        if (page == totalPages - 1) {
            persistName()
            onComplete()
        } else {
            page += 1
        }
    }

    // System back (swipe or button) steps back exactly like the in-app back
    // key. Without this the whole setup is torn down mid-flow — the swipe
    // "did nothing useful" while the on-screen key worked.
    BackHandler(enabled = page > 0) {
        page -= 1
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top strip: back · passive dots · step readout ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page > 0) {
                IconKey(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = { page -= 1 },
                    size = 40.dp,
                    tint = OffipeColors.TextSecondary
                )
            } else {
                Box(Modifier.size(40.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { i ->
                    val active = i == page
                    Box(
                        Modifier
                            .size(if (active) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) OffipeColors.Mark else OffipeColors.BorderStrong
                            )
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${page + 1} / $totalPages",
                style = OffipeType.TerminalLabel,
                color = OffipeColors.TextMuted
            )
        }

        // ── Steps: animated swap, never a swipeable pager ──
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    fadeIn(tween(240)) togetherWith fadeOut(tween(140))
                },
                label = "onboarding"
            ) { current ->
                when (current) {
                    0 -> PayOfflinePage()
                    1 -> ControlPage()
                    2 -> ReliablePage()
                    3 -> NamePage(
                        name = nameDraft,
                        onNameChange = { nameDraft = it },
                        onNext = ::next
                    )
                    4 -> PermissionsPage(permissions = permissions)
                    else -> ReadyPage(name = nameDraft)
                }
            }
        }

        // ── The one and only forward control ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.End
        ) {
            HeroFab(onClick = ::next, description = if (page == totalPages - 1) "Done" else "Next")
        }
    }
}

/** Zero-based index of the optional name step. */
private const val NAME_STEP = 3

// ── Shared step chrome ────────────────────────────────────────────────────────

/** Circular arrow key — onboarding's only forward control. */
@Composable
private fun HeroFab(onClick: () -> Unit, description: String = "Next") {
    val view = LocalView.current
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(OffipeColors.SurfaceRaised)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = description,
            tint = OffipeColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Scrollable hero shell — content only; the arrow lives on the flow. */
@Composable
private fun HeroScaffold(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        content()
        Spacer(Modifier.height(26.dp))
    }
}

/** Big reference-style headline: two short lines, tight leading. */
@Composable
private fun HeroHeadline(lines: List<String>) {
    Column {
        lines.forEachIndexed { i, line ->
            Text(
                text = line,
                style = OffipeType.DisplayLarge,
                color = OffipeColors.TextPrimary,
                lineHeight = 44.sp
            )
            if (i != lines.lastIndex) Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun HeroBody(text: String) {
    Text(
        text = text,
        style = OffipeType.BodyLarge,
        color = OffipeColors.TextSecondary
    )
}

@Composable
private fun NumberedStep(index: Int, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .background(OffipeColors.SurfaceHigher),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = OffipeType.TerminalLabel,
                color = OffipeColors.Mark
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = body,
            style = OffipeType.BodyMedium,
            color = OffipeColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Textured hero illustrations ───────────────────────────────────────────────

private enum class Art { Phone, Shield, QrScan }

/** Tilt (degrees) shared by the tilted hero art — one visual set. */
private fun artTilt(art: Art): Float = when (art) {
    Art.Phone -> -14f
    Art.Shield -> 7f
    Art.QrScan -> -12f
}

/**
 * Faint swirled backdrop behind every illustration — the reference's wavy
 * texture, kept strictly in the black/white/grey palette.
 */
private fun DrawScope.drawBackdropWaves() {
    val w = size.width
    val h = size.height
    repeat(7) { i ->
        val yBase = h * (0.10f + i * 0.13f)
        val path = Path().apply {
            moveTo(-12f, yBase)
            quadraticBezierTo(w * 0.24f, yBase - h * 0.14f, w * 0.5f, yBase)
            quadraticBezierTo(w * 0.76f, yBase + h * 0.14f, w + 12f, yBase - h * 0.05f)
        }
        drawPath(
            path = path,
            color = Color.White.copy(alpha = (0.11f - i * 0.011f).coerceAtLeast(0.02f)),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

/** Soft drop shadow that follows a hero shape's exact tilt and pivot. */
private fun DrawScope.drawHeroShadow(
    path: Path,
    tilt: Float,
    pivot: Offset,
    dx: Float = 5.dp.toPx(),
    dy: Float = 9.dp.toPx(),
    alpha: Float = 0.55f
) {
    rotate(tilt, pivot) {
        translate(dx, dy) {
            drawPath(path, Color.Black.copy(alpha = alpha))
        }
    }
}

/**
 * Paints [path] with a shaded gradient, fine diagonal hatching and speckle
 * texture, then strokes the outline — everything rotated by [tilt] around
 * [pivot]. [shade] is what gives a shape its volume: a vertical wash by
 * default, a side-lit wash for the shield.
 */
private fun DrawScope.drawTexturedShape(
    path: Path,
    tilt: Float,
    pivot: Offset,
    topColor: Color = Color(0xFF3C3C3C),
    bottomColor: Color = Color(0xFF121212),
    strokeWidth: Float = 2.dp.toPx(),
    shade: Brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
    outline: Color = OffipeColors.BorderStrong
) {
    rotate(tilt, pivot) {
        clipPath(path) {
            drawRect(shade)
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = Offset(x, 0f),
                    end = Offset(x + size.height, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += 6.dp.toPx()
            }
            repeat(56) { i ->
                val px = ((i * 61) % 101) / 100f * size.width
                val py = ((i * 37) % 97) / 96f * size.height
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = 1.6.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
        drawPath(path, color = outline, style = Stroke(width = strokeWidth))
    }
}

// ── Shared premium phone shell ───────────────────────────────────────────────

/** Corner radius of the shared phone body. */
private fun DrawScope.phoneCorner(): Float = 24.dp.toPx()

/** Recessed glass rect inside the body. */
private fun DrawScope.phoneScreenPath(left: Float, top: Float, pw: Float, ph: Float): Path {
    val inset = 7.dp.toPx()
    return Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = Rect(left + inset, top + inset, left + pw - inset, top + ph - inset),
                cornerRadius = CornerRadius(phoneCorner() - 7.dp.toPx())
            )
        )
    }
}

/**
 * One phone body used by both phone hero pages, so the set matches exactly:
 * drop shadow, textured frame, side keys, glass with scanlines, hairline rim
 * and a punch-hole camera. [screenContent] draws the UI inside the glass clip.
 */
private fun DrawScope.drawPhoneShell(
    left: Float,
    top: Float,
    pw: Float,
    ph: Float,
    tilt: Float,
    pivot: Offset,
    screenContent: DrawScope.() -> Unit = {}
) {
    val body = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = Rect(left, top, left + pw, top + ph),
                cornerRadius = CornerRadius(phoneCorner())
            )
        )
    }

    drawHeroShadow(body, tilt, pivot, dx = 6.dp.toPx(), dy = 11.dp.toPx(), alpha = 0.6f)
    drawTexturedShape(
        path = body,
        tilt = tilt,
        pivot = pivot,
        topColor = Color(0xFF4A4A4A),
        bottomColor = Color(0xFF0B0B0B),
        strokeWidth = 2.dp.toPx()
    )

    val inset = 7.dp.toPx()
    val screen = phoneScreenPath(left, top, pw, ph)

    rotate(tilt, pivot) {
        // Side keys — volume + power, peeking past the frame
        val keyW = 3.5.dp.toPx()
        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(left + pw - 1.dp.toPx(), top + ph * 0.24f),
            size = Size(keyW, 26.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(left + pw - 1.dp.toPx(), top + ph * 0.37f),
            size = Size(keyW, 44.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        clipPath(screen) {
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF232323), Color(0xFF0F0F0F)))
            )
            var y = top + inset
            while (y < top + ph - inset) {
                drawLine(
                    color = Color.White.copy(alpha = 0.045f),
                    start = Offset(left + inset, y),
                    end = Offset(left + pw - inset, y),
                    strokeWidth = 1f
                )
                y += 9.dp.toPx()
            }
            screenContent()
        }

        // Glass rim
        drawPath(screen, Color.White.copy(alpha = 0.12f), style = Stroke(width = 1f))

        // Punch-hole camera
        val hole = Offset(left + pw / 2f, top + inset + 11.dp.toPx())
        drawCircle(Color(0xFF050505), radius = 4.dp.toPx(), center = hole)
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = 4.dp.toPx(),
            center = hole,
            style = Stroke(width = 1.5f)
        )
    }
}

/** Status bar + title + code plate + list rows + action pill (phone screen). */
private fun DrawScope.drawPayScreen(left: Float, top: Float, pw: Float, ph: Float) {
    val inset = 7.dp.toPx()
    val sx = left + inset
    val sy = top + inset
    val iw = pw - 2 * inset
    val ih = ph - 2 * inset
    val dp = 1.dp.toPx()

    drawRoundRect(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = Offset(sx + 9 * dp, sy + 8 * dp),
        size = Size(16 * dp, 3.5f * dp),
        cornerRadius = CornerRadius(2 * dp)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = Offset(sx + iw - 24 * dp, sy + 8 * dp),
        size = Size(15 * dp, 3.5f * dp),
        cornerRadius = CornerRadius(2 * dp)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset(sx + 9 * dp, sy + 21 * dp),
        size = Size(iw * 0.5f, 6 * dp),
        cornerRadius = CornerRadius(3 * dp)
    )

    // Code plate — the `*99#` overlay lands exactly on this
    val plateH = 40 * dp
    val plateTop = sy + ih / 2f - plateH / 2f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.07f),
        topLeft = Offset(sx + 9 * dp, plateTop),
        size = Size(iw - 18 * dp, plateH),
        cornerRadius = CornerRadius(6 * dp)
    )

    listOf(0.9f, 0.64f, 0.76f).forEachIndexed { i, f ->
        drawRoundRect(
            color = Color.White.copy(alpha = 0.065f),
            topLeft = Offset(sx + 9 * dp, plateTop + plateH + 10 * dp + i * 12 * dp),
            size = Size((iw - 18 * dp) * f, 5 * dp),
            cornerRadius = CornerRadius(2.5f * dp)
        )
    }
    drawRoundRect(
        color = Color.White.copy(alpha = 0.13f),
        topLeft = Offset(sx + 9 * dp, sy + ih - 27 * dp),
        size = Size(iw - 18 * dp, 17 * dp),
        cornerRadius = CornerRadius(8.5f * dp)
    )
}

/** Viewfinder UI for the scanning phone screen: dim, target QR, brackets. */
private fun DrawScope.drawScanScreen(left: Float, top: Float, pw: Float, ph: Float) {
    val inset = 7.dp.toPx()
    val sx = left + inset
    val sy = top + inset
    val iw = pw - 2 * inset
    val ih = ph - 2 * inset
    val dp = 1.dp.toPx()

    drawRect(Color.Black.copy(alpha = 0.45f), Offset(sx, sy), Size(iw, ih))
    drawRoundRect(
        color = Color.White.copy(alpha = 0.14f),
        topLeft = Offset(sx + 9 * dp, sy + 8 * dp),
        size = Size(15 * dp, 3.5f * dp),
        cornerRadius = CornerRadius(2 * dp)
    )

    // Target QR inside the viewfinder
    val t = iw * 0.60f
    val tx = sx + (iw - t) / 2f
    val ty = sy + ih * 0.42f - t / 2f
    drawQrModules(tx, ty, t, cells = 11)

    // Focus brackets around it
    val gap = 6 * dp
    val len = t * 0.26f
    val bw = 2.5f * dp
    val bx = tx - gap
    val by = ty - gap
    val br = tx + t + gap
    val bb = ty + t + gap
    val bracket = Color.White.copy(alpha = 0.85f)
    listOf(
        Triple(Offset(bx, by), Offset(bx + len, by), Offset(bx, by + len)),
        Triple(Offset(br, by), Offset(br - len, by), Offset(br, by + len)),
        Triple(Offset(bx, bb), Offset(bx + len, bb), Offset(bx, bb - len)),
        Triple(Offset(br, bb), Offset(br - len, bb), Offset(br, bb - len))
    ).forEach { (corner, hEnd, vEnd) ->
        drawLine(bracket, corner, hEnd, strokeWidth = bw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(bracket, corner, vEnd, strokeWidth = bw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }

    // Caption bars + action pill
    drawRoundRect(
        color = Color.White.copy(alpha = 0.12f),
        topLeft = Offset(sx + 11 * dp, sy + ih * 0.72f),
        size = Size(iw - 22 * dp, 5 * dp),
        cornerRadius = CornerRadius(2.5f * dp)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.08f),
        topLeft = Offset(sx + 11 * dp, sy + ih * 0.72f + 11 * dp),
        size = Size((iw - 22 * dp) * 0.66f, 4 * dp),
        cornerRadius = CornerRadius(2 * dp)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.13f),
        topLeft = Offset(sx + 9 * dp, sy + ih - 27 * dp),
        size = Size(iw - 18 * dp, 17 * dp),
        cornerRadius = CornerRadius(8.5f * dp)
    )
}

/**
 * Deterministic QR-looking module grid: three finder patterns plus a stable
 * pseudo-random data field. Decorative only — it stands in for a real code
 * in the hero art.
 */
private fun DrawScope.drawQrModules(x: Float, y: Float, side: Float, cells: Int = 21) {
    if (cells < 8) return
    val m = side / cells
    val ink = Color(0xFF111111)
    val inFinder = { r: Int, c: Int ->
        (r < 8 && c < 8) || (r < 8 && c >= cells - 8) || (r >= cells - 8 && c < 8)
    }
    for (r in 0 until cells) {
        for (c in 0 until cells) {
            if (inFinder(r, c)) continue
            if ((r * 31 + c * 17 + r * c * 7) % 5 <= 1) {
                drawRect(
                    color = ink,
                    topLeft = Offset(x + c * m, y + r * m),
                    size = Size(m * 0.94f, m * 0.94f)
                )
            }
        }
    }
    listOf(0 to 0, 0 to (cells - 7), (cells - 7) to 0).forEach { (fr, fc) ->
        val fx = x + fc * m
        val fy = y + fr * m
        drawRect(
            color = ink,
            topLeft = Offset(fx + m / 2f, fy + m / 2f),
            size = Size(6 * m, 6 * m),
            style = Stroke(width = m)
        )
        drawRect(
            color = ink,
            topLeft = Offset(fx + 2 * m, fy + 2 * m),
            size = Size(3 * m, 3 * m)
        )
    }
}

// ── The three hero pages ─────────────────────────────────────────────────────

/** Page 1 — premium phone running the `*99#` flow, USSD arcs leaving it. */
private fun DrawScope.drawPhoneHero(tilt: Float) {
    val w = size.width
    val h = size.height
    val ph = h * 0.86f
    val pw = ph * 0.47f
    val left = w / 2f - pw / 2f
    val top = h / 2f - ph / 2f
    val pivot = Offset(w / 2f, h / 2f)

    drawPhoneShell(left, top, pw, ph, tilt, pivot) {
        drawPayScreen(left, top, pw, ph)
    }

    // Signal arcs — the `*99#` channel leaving the phone
    rotate(tilt, pivot) {
        val cx = left + pw + 14.dp.toPx()
        val cy = h / 2f
        listOf(16.dp, 27.dp, 38.dp).forEachIndexed { i, r ->
            val start = Math.toRadians(-55.0)
            val sweep = Math.toRadians(110.0)
            val arc = Path().apply {
                moveTo(
                    cx + (cos(start) * r.toPx()).toFloat(),
                    cy + (sin(start) * r.toPx()).toFloat()
                )
                for (step in 1..24) {
                    val a = start + sweep * (step / 24.0)
                    lineTo(
                        cx + (cos(a) * r.toPx()).toFloat(),
                        cy + (sin(a) * r.toPx()).toFloat()
                    )
                }
            }
            drawPath(
                path = arc,
                color = if (i == 2) OffipeColors.TextSecondary else OffipeColors.BorderStrong,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
    }
}

/** Shield outline built from its bounding metrics (so it can be inset for a bevel). */
private fun shieldPath(cx: Float, top: Float, bot: Float, halfW: Float): Path {
    // Same proportions as the original construction, expressed against the
    // shape's own height so scaling top/bot/halfW scales every control point.
    val d = bot - top
    return Path().apply {
        moveTo(cx, top)
        lineTo(cx + halfW, top + d * 0.2195f)
        lineTo(cx + halfW, top + d * 0.5122f)
        quadraticBezierTo(cx + halfW, bot - d * 0.0732f, cx, bot)
        quadraticBezierTo(cx - halfW, bot - d * 0.0732f, cx - halfW, top + d * 0.5122f)
        lineTo(cx - halfW, top + d * 0.2195f)
        close()
    }
}

/** Page 2 — dimensional shield: side-lit volume, bevel, spine, checked face. */
private fun DrawScope.drawShieldHero(tilt: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val top = h * 0.10f
    val bot = h * 0.92f
    val halfW = h * 0.34f
    val pivot = Offset(w / 2f, h / 2f)

    val path = shieldPath(cx, top, bot, halfW)

    drawHeroShadow(path, tilt, pivot, dx = 7.dp.toPx(), dy = 11.dp.toPx(), alpha = 0.5f)
    drawTexturedShape(
        path = path,
        tilt = tilt,
        pivot = pivot,
        // Light from the left: the face reads as a volume, not a flat cut-out
        shade = Brush.horizontalGradient(
            listOf(Color(0xFF5E5E5E), Color(0xFF242424), Color(0xFF0B0B0B))
        ),
        strokeWidth = 2.dp.toPx()
    )

    rotate(tilt, pivot) {
        val cy = (top + bot) / 2f
        val inner = shieldPath(
            cx = cx,
            top = cy + (top - cy) * 0.93f,
            bot = cy + (bot - cy) * 0.93f,
            halfW = halfW * 0.93f
        )
        clipPath(path) {
            // Bevel catching the light + a centre spine
            drawPath(
                inner,
                color = Color.White.copy(alpha = 0.16f),
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(cx, top + h * 0.04f),
                end = Offset(cx, bot - h * 0.05f),
                strokeWidth = 1.4.dp.toPx()
            )
            // Check, with a drop shadow for depth
            val shadow = 2.4.dp.toPx()
            listOf(
                Offset(cx - 20.dp.toPx(), h * 0.50f) to Offset(cx - 6.dp.toPx(), h * 0.63f),
                Offset(cx - 6.dp.toPx(), h * 0.63f) to Offset(cx + 26.dp.toPx(), h * 0.34f)
            ).forEach { (a, b) ->
                drawLine(
                    color = Color.Black.copy(alpha = 0.45f),
                    start = Offset(a.x, a.y + shadow),
                    end = Offset(b.x, b.y + shadow),
                    strokeWidth = 5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            listOf(
                Offset(cx - 20.dp.toPx(), h * 0.50f) to Offset(cx - 6.dp.toPx(), h * 0.63f),
                Offset(cx - 6.dp.toPx(), h * 0.63f) to Offset(cx + 26.dp.toPx(), h * 0.34f)
            ).forEach { (a, b) ->
                drawLine(
                    color = OffipeColors.TextPrimary,
                    start = a,
                    end = b,
                    strokeWidth = 5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

/** Page 3 — the same phone, now aiming at a QR card under focus brackets. */
private fun DrawScope.drawQrScanHero(tilt: Float) {
    val w = size.width
    val h = size.height

    // Phone, upper-left
    val ph = h * 0.74f
    val pw = ph * 0.47f
    val pcx = w * 0.36f
    val pcy = h * 0.40f
    val left = pcx - pw / 2f
    val top = pcy - ph / 2f
    val pivot = Offset(pcx, pcy)
    drawPhoneShell(left, top, pw, ph, tilt, pivot) {
        drawScanScreen(left, top, pw, ph)
    }

    // QR card, lower-right, with the scanner's focus brackets
    val card = h * 0.40f
    val ccx = w * 0.64f
    val ccy = h * 0.66f
    val cl = ccx - card / 2f
    val ct = ccy - card / 2f
    val cardTilt = -6f

    rotate(cardTilt, Offset(ccx, ccy)) {
        translate(4.dp.toPx(), 8.dp.toPx()) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(cl, ct),
                size = Size(card, card),
                cornerRadius = CornerRadius(14.dp.toPx())
            )
        }
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFFF5F5F5), Color(0xFFD6D6D6))),
            topLeft = Offset(cl, ct),
            size = Size(card, card),
            cornerRadius = CornerRadius(14.dp.toPx())
        )
        val quiet = 10.dp.toPx()
        drawQrModules(cl + quiet, ct + quiet, card - 2 * quiet, cells = 21)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(cl, ct),
            size = Size(card, card),
            cornerRadius = CornerRadius(14.dp.toPx()),
            style = Stroke(width = 1f)
        )

        // Focus brackets around the card
        val gap = 7.dp.toPx()
        val len = card * 0.20f
        val bw = 3.dp.toPx()
        val bx = cl - gap
        val by = ct - gap
        val br = cl + card + gap
        val bb = ct + card + gap
        val bracket = OffipeColors.TextPrimary.copy(alpha = 0.9f)
        listOf(
            Triple(Offset(bx, by), Offset(bx + len, by), Offset(bx, by + len)),
            Triple(Offset(br, by), Offset(br - len, by), Offset(br, by + len)),
            Triple(Offset(bx, bb), Offset(bx + len, bb), Offset(bx, bb - len)),
            Triple(Offset(br, bb), Offset(br - len, bb), Offset(br, bb - len))
        ).forEach { (corner, hEnd, vEnd) ->
            drawLine(bracket, corner, hEnd, strokeWidth = bw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(bracket, corner, vEnd, strokeWidth = bw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

/**
 * Tilted illustration for each hero page — all vector, all one family:
 * the premium phone (pages 1 and 3), the dimensional shield (page 2) and
 * the phone locked onto a QR (page 3).
 */
@Composable
private fun HeroArt(art: Art, modifier: Modifier = Modifier) {
    val tilt = artTilt(art)
    Box(
        modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawBackdropWaves()
            when (art) {
                Art.Phone -> drawPhoneHero(tilt)
                Art.Shield -> drawShieldHero(tilt)
                Art.QrScan -> drawQrScanHero(tilt)
            }
        }

        if (art == Art.Phone) {
            Text(
                text = "*99#",
                style = OffipeType.AmountLg.copy(
                    color = OffipeColors.TextPrimary,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier.graphicsLayer { rotationZ = tilt }
            )
        }
    }
}

// ── Page 1: Pay Offline ───────────────────────────────────────────────────────

@Composable
private fun PayOfflinePage() {
    HeroScaffold {
        HeroHeadline(listOf("Pay", "Offline."))
        Spacer(Modifier.height(16.dp))
        HeroBody("Payments without internet or mobile data.")
        Spacer(Modifier.height(22.dp))
        HeroArt(Art.Phone)

    }
}

// ── Page 2: Your Money. Your Control. ────────────────────────────────────────

@Composable
private fun ControlPage() {
    val explainer = buildAnnotatedString {
        withStyle(SpanStyle(fontFamily = OffipeType.MonoFamily)) { append("*99#") }
        append(
            " is NPCI's USSD shortcode for offline UPI. It rides over your phone's " +
                "signaling channel — the same one that carries calls and SMS — so it " +
                "works in low-coverage areas where mobile data fails."
        )
    }

    HeroScaffold {
        HeroHeadline(listOf("Your Money.", "Your Control."))
        Spacer(Modifier.height(16.dp))
        HeroBody("Secure, private and designed for everyday payments.")
        Spacer(Modifier.height(18.dp))
        HeroArt(Art.Shield)
        Spacer(Modifier.height(18.dp))
        Text(
            text = explainer,
            style = OffipeType.BodyMedium,
            color = OffipeColors.TextSecondary
        )

        Spacer(Modifier.height(14.dp))
        Tag("Carrier support", color = OffipeColors.TextMuted)
        Spacer(Modifier.height(6.dp))
        Hairline()
        CarrierRow("AIRTEL", supported = true)
        Hairline()
        CarrierRow("VI", supported = true)
        Hairline()
        CarrierRow("BSNL", supported = true)
        Hairline()
        CarrierRow("JIO", supported = false)
        Hairline()
    }
}

@Composable
private fun CarrierRow(name: String, supported: Boolean) {
    val state = if (supported) OffipeColors.Success else OffipeColors.Danger
    ReadoutRow(
        label = name,
        value = if (supported) "OK" else "NO",
        valueColor = state,
        leading = { LedDot(size = 6.dp, color = state) }
    )
}

// ── Page 3: One-time *99# setup ──────────────────────────────────────────────

@Composable
private fun ReliablePage() {
    HeroScaffold {
        HeroHeadline(listOf("Fast.", "Simple.", "Reliable."))
        Spacer(Modifier.height(14.dp))
        HeroBody("Scan a UPI QR and pay — even without mobile data.")
        Spacer(Modifier.height(18.dp))
        HeroArt(Art.QrScan)

        Spacer(Modifier.height(16.dp))
        Tag("One-time setup", color = OffipeColors.Mark)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Link your bank to the *99# channel once. Offipe handles the rest, " +
                "online or off.",
            style = OffipeType.BodyMedium,
            color = OffipeColors.TextSecondary
        )

    }
}

// ── Page 4: Name ─────────────────────────────────────────────────────────────

/**
 * Optional preferred-name capture. Blank input is valid — the greeting on
 * Home simply falls back to time-only wording. The value is sanitised by
 * [UserNameRules.sanitize] before it is persisted.
 */
@Composable
private fun NamePage(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    HeroScaffold {
        HeroHeadline(listOf("What should", "we call you?"))
        Spacer(Modifier.height(16.dp))
        HeroBody("We'll use it to say hello on Home. Leave it blank to skip — " +
            "you can set it later.")
        Spacer(Modifier.height(26.dp))

        InlineField(
            value = name,
            onValueChange = { raw ->
                // Rejected outright rather than truncated mid-word.
                if (raw.length <= UserNameRules.MAX_LENGTH) onNameChange(raw)
            },
            label = "YOUR NAME",
            placeholder = "e.g. Rahul",
            modifier = Modifier.fillMaxWidth(),
            imeAction = androidx.compose.ui.text.input.ImeAction.Done
        )

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (name.isBlank()) "Optional" else "Saved when you continue",
                style = OffipeType.TerminalLabel,
                color = OffipeColors.TextMuted
            )
            Text(
                text = "${name.length}/${UserNameRules.MAX_LENGTH}",
                style = OffipeType.TerminalLabel,
                color = OffipeColors.TextMuted
            )
        }

        Spacer(Modifier.height(22.dp))
        TextKey(text = "Skip for now", color = OffipeColors.TextSecondary, onClick = {
            onNameChange("")
            onNext()
        })
    }
}

// ── Page 5: Permissions ───────────────────────────────────────────────────────

private data class PermissionInfo(
    val title: String,
    val short: String,
    val why: String,
    val granted: Boolean,
    val onGrant: () -> Unit,
    /** When true, the row shows an inline restricted-settings fix below it. */
    val showFaqHint: Boolean = false
)

@Composable
private fun PermissionsPage(permissions: PermissionStatus) {
    val context = LocalContext.current
    val launchers = rememberPermissionLaunchers()

    val items = listOf(
        PermissionInfo(
            title = "Phone",
            short = "Dial *99# and read the carrier name.",
            why = "Offipe dials *99# through Android's call API to start each USSD " +
                "session, and reads the active SIM's carrier name to warn you if " +
                "you're on Jio (where *99# is unreliable).",
            granted = permissions.phoneBundle,
            onGrant = { launchers.requestPhoneBundle() }
        ),
        PermissionInfo(
            title = "Camera",
            short = "Scan UPI QR codes.",
            why = "We use the camera to scan UPI QR codes and autofill the recipient's " +
                "UPI ID and amount. You can also pick QR images from your gallery — " +
                "that doesn't need camera access.",
            granted = permissions.camera,
            onGrant = { launchers.requestCamera() }
        ),
        PermissionInfo(
            title = "Accessibility",
            short = "Read and reply to the carrier's USSD dialog.",
            why = "Android's only public USSD API can't navigate multi-step menus. " +
                "The accessibility service is what lets Offipe automatically type " +
                "your amount, VPA and PIN into the carrier's dialog so you don't have to.",
            granted = permissions.accessibility,
            onGrant = { openAccessibilitySettings(context) },
            showFaqHint = true
        ),
        PermissionInfo(
            title = "Display Over Other Apps",
            short = "Cover the system dialog with Offipe's overlay.",
            why = "In Auto mode, Offipe paints a branded overlay over the system USSD " +
                "dialog so you only ever see Offipe's UI during a session. In Advanced " +
                "mode, the overlay is a small chip pinned under the status bar.",
            granted = permissions.overlay,
            onGrant = { openOverlaySettings(context) }
        )
    )

    var helpFor by remember { mutableStateOf<PermissionInfo?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Tag("Permissions", color = OffipeColors.TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Permissions",
            style = OffipeType.DisplayMedium,
            color = OffipeColors.TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Step 5 of 6",
            style = OffipeType.TerminalLabel,
            color = OffipeColors.TextMuted
        )

        Spacer(Modifier.height(16.dp))

        Hairline()
        items.forEach { info ->
            PermissionRow(
                info = info,
                onHelp = { helpFor = info }
            )
            Hairline()
        }

        val ungranted = items.count { !it.granted }
        if (ungranted > 0) {
            Spacer(Modifier.height(14.dp))
            AlertStrip(
                title = "$ungranted not granted",
                message = "Some features won't work until they are.",
                tone = AlertTone.Warn,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
    }

    helpFor?.let { info ->
        WhyWeNeedDialog(info = info, onDismiss = { helpFor = null })
    }
}

@Composable
private fun PermissionRow(info: PermissionInfo, onHelp: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 13.dp, bottom = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedDot(
                size = 6.dp,
                color = if (info.granted) OffipeColors.Success else OffipeColors.Warn,
                pulse = !info.granted
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = info.title,
                style = OffipeType.LabelMedium,
                color = OffipeColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            if (info.granted) {
                Tag("Granted", color = OffipeColors.Success)
            } else {
                TextKey(text = "Grant", onClick = info.onGrant, color = OffipeColors.Mark)
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(18.dp))
            Text(
                text = info.short,
                style = OffipeType.BodySmall,
                color = OffipeColors.TextMuted,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconKey(
                icon = Icons.Default.HelpOutline,
                contentDescription = "Why ${info.title} is needed",
                onClick = onHelp,
                size = 30.dp,
                tint = OffipeColors.TextMuted
            )
        }

        if (info.showFaqHint && !info.granted) {
            Spacer(Modifier.height(8.dp))
            RestrictedSettingsFix()
        }
    }
}

/** Inline "why we need this" prompt for a single permission. */
@Composable
private fun WhyWeNeedDialog(info: PermissionInfo, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        OffipeDialogSurface {
            Column(Modifier.padding(top = 22.dp, start = 22.dp, end = 22.dp)) {
                Tag("Why we need this · ${info.title}", color = OffipeColors.Mark)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = info.title,
                    style = OffipeType.DisplaySmall,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = info.why,
                    style = OffipeType.BodyLarge,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(22.dp))
            }
            GhostActionBar(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
        }
    }
}

/**
 * Expandable inline fix for Android 13+ "Restricted Settings" that prevents
 * enabling accessibility for sideloaded apps. Shows a tappable "Can't enable?"
 * label that expands to reveal step-by-step instructions + guide link.
 */
@Composable
private fun RestrictedSettingsFix() {
    val context = LocalContext.current
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = OffipeColors.Warn.copy(alpha = 0.55f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .padding(start = 14.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    expanded = !expanded
                }
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tag(
                text = if (expanded) "Hide fix" else "Can't enable? Tap for fix",
                color = OffipeColors.Warn,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (expanded) "Hide fix" else "Show fix",
                tint = OffipeColors.TextMuted,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = if (expanded) 90f else 0f }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = "Why does this happen?",
                    style = OffipeType.LabelSmall,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Since Offipe is sideloaded (not from Play Store), Android 13+ " +
                        "blocks restricted settings like Accessibility by default. This is " +
                        "the same reason you had to disable Play Protect — it's a mandatory " +
                        "Google security measure for sideloaded apps.",
                    style = OffipeType.BodySmall,
                    color = OffipeColors.TextMuted
                )
                Spacer(Modifier.height(12.dp))

                Tag("Fix", color = OffipeColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                NumberedStep(1, "Go to your phone's Settings → Apps → Offipe")
                Spacer(Modifier.height(6.dp))
                NumberedStep(2, "Tap the ⋮ three dots in the top right corner")
                Spacer(Modifier.height(6.dp))
                NumberedStep(3, "Select \"Allow restricted settings\" and confirm with your PIN/fingerprint")
                Spacer(Modifier.height(6.dp))
                NumberedStep(4, "Now open Offipe and enable the Accessibility service — it will work cleanly")

                Spacer(Modifier.height(14.dp))
                TextKey(
                    text = "View guide with screenshots",
                    color = OffipeColors.Mark,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(GITHUB_REPO_URL)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }
                )
            }
        }
    }
}

// ── Page 6: Ready ─────────────────────────────────────────────────────────────

@Composable
private fun ReadyPage(name: String) {
    val who = name.trim()
    HeroScaffold {
        Spacer(Modifier.height(70.dp))
        Text(
            text = "You're set.",
            style = OffipeType.DisplayLarge,
            color = OffipeColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (who.isEmpty()) {
                "Everything is ready. Time to pay."
            } else {
                "Everything is ready, $who. Time to pay."
            },
            style = OffipeType.BodyLarge,
            color = OffipeColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) {
                LedDot(size = 7.dp, color = OffipeColors.Success)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Tag("Offline UPI · *99#", color = OffipeColors.TextMuted)
        }
    }
}
