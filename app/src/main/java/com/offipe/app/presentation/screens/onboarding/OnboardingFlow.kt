package com.offipe.app.presentation.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.offipe.app.R
import com.offipe.app.presentation.permissions.PermissionStatus
import com.offipe.app.presentation.permissions.openAccessibilitySettings
import com.offipe.app.presentation.permissions.openOverlaySettings
import com.offipe.app.presentation.permissions.rememberPermissionLaunchers
import com.offipe.app.presentation.permissions.rememberPermissionStatus
import com.offipe.app.presentation.ui.components.OffipeCard
import com.offipe.app.presentation.ui.components.OffipePrimaryButton
import com.offipe.app.presentation.ui.components.OffipeSecondaryButton
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import kotlinx.coroutines.launch

// ─── Hooks for the user to drop in real assets later ──────────────────────────

/**
 * Tutorial video URL. When non-null, the welcome page (page 1) renders a
 * small "Watch tutorial" link below the hero CTA.
 */
private val TUTORIAL_VIDEO_URL: String? = "https://youtube.com/playlist?list=PL6zhuU_l94t1y25MDt96Z-MltD3S6iPFj&si=GNlanTwR-IcfOBI"

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(onComplete: () -> Unit) {
    val totalPages = 5
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    val permissionsState = rememberPermissionStatus()
    val permissions = permissionsState.value

    Column(
        Modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
            .statusBarsPadding()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> Star99ExplainerPage()
                2 -> Star99BankingSetupPage()
                3 -> PermissionsPage(permissions = permissions)
                4 -> ReadyPage()
            }
        }
        Spacer(Modifier.height(8.dp))
        PageIndicator(current = pagerState.currentPage, total = totalPages)
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            val isLast = pagerState.currentPage == totalPages - 1
            OffipePrimaryButton(
                text = when (pagerState.currentPage) {
                    0 -> "Continue"
                    1 -> "Got It"
                    2 -> "Next"
                    3 -> if (permissions.readyForOverlayPay) "Looks Good" else "Continue Anyway"
                    else -> "Let's Pay"
                },
                onClick = {
                    if (isLast) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PageIndicator(current: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(total) { i ->
            val isActive = i == current
            // 6dp dot inactive, 24dp lime pill active. Spring animation
            // smooths the transition between states.
            val width by animateDpAsState(
                targetValue = if (isActive) 24.dp else 6.dp,
                animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
                label = "indicator_w_$i"
            )
            val height by animateDpAsState(
                targetValue = if (isActive) 6.dp else 6.dp,
                animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
                label = "indicator_h_$i"
            )
            Box(
                Modifier
                    .size(width = width, height = height)
                    .background(if (isActive) OffipeColors.Accent else OffipeColors.Border)
            )
            if (i < total - 1) Spacer(Modifier.width(6.dp))
        }
    }
}

// ── Page 1: Welcome ──

@Composable
private fun WelcomePage() {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition(label = "welcome")
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(14.dp)
                    .alpha(pulse)
                    .background(OffipeColors.Accent)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "OFFLINE UPI",
                style = OffipeType.LabelMedium,
                color = OffipeColors.Accent
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Offipe",
            style = OffipeType.DisplayLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit(64f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = OffipeColors.TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pay over UPI without internet. Just *99# and a phone signal.",
            style = OffipeType.BodyLarge,
            color = OffipeColors.TextSecondary
        )
        // Optional inline link to a tutorial video — only renders when the
        // top-of-file constant is set.
        TUTORIAL_VIDEO_URL?.let { url ->
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = OffipeColors.Accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Watch tutorial",
                    style = OffipeType.LabelMedium,
                    color = OffipeColors.Accent
                )
            }
        }
    }
}

// ── Page 2: What is *99# ──

@Composable
private fun Star99ExplainerPage() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "WHY *99#",
            style = OffipeType.LabelMedium,
            color = OffipeColors.Accent
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "USSD makes UPI work without data.",
            style = OffipeType.DisplayMedium,
            color = OffipeColors.TextPrimary
        )
        Spacer(Modifier.height(20.dp))
        OffipeCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "*99# is NPCI's USSD shortcode for offline UPI. It rides over your phone's signaling channel — the same one that carries calls and SMS — so it works in low-coverage areas where mobile data fails.",
                    style = OffipeType.BodyLarge,
                    color = OffipeColors.TextSecondary
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CarrierPill("AIRTEL", supported = true, modifier = Modifier.weight(1f))
            CarrierPill("VI", supported = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CarrierPill("BSNL", supported = true, modifier = Modifier.weight(1f))
            CarrierPill("JIO", supported = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CarrierPill(name: String, supported: Boolean, modifier: Modifier = Modifier) {
    val accent = if (supported) OffipeColors.Success else OffipeColors.Danger
    Box(
        modifier
            .background(OffipeColors.SurfaceHigh)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(accent))
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                style = OffipeType.LabelMedium,
                color = OffipeColors.TextPrimary
            )
        }
    }
}

// ── Page 3: *99# Banking Setup (NEW) ──

@Composable
private fun Star99BankingSetupPage() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // ── One-time badge ──
        Box(
            Modifier
                .background(OffipeColors.Accent.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "⚡ ONE-TIME SETUP",
                style = OffipeType.LabelSmall,
                color = OffipeColors.Accent
            )
        }
        Spacer(Modifier.height(12.dp))

        Text(
            text = "ENABLE BANKING",
            style = OffipeType.LabelMedium,
            color = OffipeColors.Accent
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Make sure you have enabled banking via *99#",
            style = OffipeType.DisplayMedium,
            color = OffipeColors.TextPrimary
        )
        Spacer(Modifier.height(20.dp))

        // ── Instructions card ──
        OffipeCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "This needs to be done once. After this, Offipe handles everything.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(16.dp))

                NumberedStep(1, "Go to your phone's dialer and dial *99#")
                Spacer(Modifier.height(12.dp))
                NumberedStep(2, "Enter your bank name when prompted (e.g. SBI, HDFC, ICICI)")
                Spacer(Modifier.height(12.dp))
                NumberedStep(3, "Follow the on-screen prompts to link your bank account")
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Dial *99# button ──
        OffipePrimaryButton(
            text = "Dial *99#",
            leadingIcon = Icons.Default.Phone,
            onClick = {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:*99${Uri.encode("#")}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(dialIntent) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ── Video guide link ──
        OffipeSecondaryButton(
            text = "Watch Official *99# Guide",
            leadingIcon = Icons.Default.PlayArrow,
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.bhimupi.org.in/steps-to-use-99#")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // ── Info note ──
        Box(
            Modifier
                .fillMaxWidth()
                .background(OffipeColors.SurfaceHigh)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = OffipeColors.Accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "This is a one-time process to enable your bank on the *99# USSD channel. Once done, you won't need to do this again.",
                    style = OffipeType.BodySmall,
                    color = OffipeColors.TextMuted
                )
            }
        }
    }
}



@Composable
private fun NumberedStep(index: Int, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(24.dp)
                .background(OffipeColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = OffipeType.LabelMedium,
                color = OffipeColors.Black
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

@Composable
private fun ImageSlot(res: Int?) {
    Spacer(Modifier.height(10.dp))
    if (res != null) {
        // Real screenshot: scale-to-fit, capped at 480dp tall so the portrait
        // BHIM screenshots don't dominate the screen on small devices.
        Image(
            painter = painterResource(id = res),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
        )
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(OffipeColors.Surface)
                .drawBehind {
                    drawRect(
                        color = OffipeColors.Accent.copy(alpha = 0.4f),
                        style = Stroke(width = 1f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "step image",
                style = OffipeType.LabelMedium,
                color = OffipeColors.TextMuted
            )
        }
    }
}

// ── Page 4: Permissions ──

private data class PermissionInfo(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val short: String,
    val why: String,
    val granted: Boolean,
    val onGrant: () -> Unit,
    /** When true, the GRANT button shows an inline FAQ-link hint below it. */
    val showFaqHint: Boolean = false
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsPage(permissions: PermissionStatus) {
    val context = LocalContext.current
    val launchers = rememberPermissionLaunchers()

    val items = listOf(
        PermissionInfo(
            key = "phone",
            icon = Icons.Default.Phone,
            title = "Phone",
            short = "Dial *99# and read the carrier name.",
            why = "Offipe dials *99# through Android's call API to start each USSD session, and reads the active SIM's carrier name to warn you if you're on Jio (where *99# is unreliable).",
            granted = permissions.phoneBundle,
            onGrant = { launchers.requestPhoneBundle() }
        ),
        PermissionInfo(
            key = "camera",
            icon = Icons.Default.Camera,
            title = "Camera",
            short = "Scan UPI QR codes.",
            why = "We use the camera to scan UPI QR codes and autofill the recipient's UPI ID and amount. You can also pick QR images from your gallery — that doesn't need camera access.",
            granted = permissions.camera,
            onGrant = { launchers.requestCamera() }
        ),
        PermissionInfo(
            key = "accessibility",
            icon = Icons.Default.Settings,
            title = "Accessibility",
            short = "Read and reply to the carrier's USSD dialog.",
            why = "Android's only public USSD API can't navigate multi-step menus. The accessibility service is what lets Offipe automatically type your amount, VPA and PIN into the carrier's dialog so you don't have to.",
            granted = permissions.accessibility,
            onGrant = { openAccessibilitySettings(context) },
            // Android 13+ may grey out the toggle. The FAQ has the workaround
            // — surface a tappable hint right next to the GRANT button.
            showFaqHint = true
        ),
        PermissionInfo(
            key = "overlay",
            icon = Icons.Default.Layers,
            title = "Display Over Other Apps",
            short = "Cover the system dialog with Offipe's overlay.",
            why = "In Auto mode, Offipe paints a branded overlay over the system USSD dialog so you only ever see Offipe's UI during a session. In Advanced mode, the overlay is a small chip pinned under the status bar.",
            granted = permissions.overlay,
            onGrant = { openOverlaySettings(context) }
        )
    )

    val view = LocalView.current
    var helpFor by remember { mutableStateOf<PermissionInfo?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "PERMISSIONS",
            style = OffipeType.LabelMedium,
            color = OffipeColors.Accent
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Offipe needs four things.",
            style = OffipeType.DisplayMedium,
            color = OffipeColors.TextPrimary
        )

        val ungranted = items.count { !it.granted }
        if (ungranted > 0) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .background(OffipeColors.Warn.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "$ungranted not granted — some features won't work until they are.",
                    style = OffipeType.BodySmall,
                    color = OffipeColors.Warn
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        items.forEach { info ->
            PermissionCard(
                info = info,
                onHelp = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    helpFor = info
                }
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(20.dp))
    }

    helpFor?.let { info ->
        ModalBottomSheet(
            onDismissRequest = { helpFor = null },
            sheetState = sheetState,
            containerColor = OffipeColors.SurfaceHigh
        ) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text(
                    text = info.title.uppercase(),
                    style = OffipeType.LabelMedium,
                    color = OffipeColors.Accent
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Why we need this",
                    style = OffipeType.DisplaySmall,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = info.why,
                    style = OffipeType.BodyLarge,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(info: PermissionInfo, onHelp: () -> Unit) {
    OffipeCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(OffipeColors.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = info.icon,
                        contentDescription = null,
                        tint = if (info.granted) OffipeColors.Accent else OffipeColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = info.title,
                            style = OffipeType.TitleLarge,
                            color = OffipeColors.TextPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(20.dp)
                                .clickable { onHelp() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help",
                                tint = OffipeColors.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = info.short,
                        style = OffipeType.BodySmall,
                        color = OffipeColors.TextMuted
                    )
                }
                Spacer(Modifier.width(10.dp))
                StatusButton(granted = info.granted, onGrant = info.onGrant)
            }
            // Inline "Can't enable?" expandable fix for restricted settings.
            if (info.showFaqHint && !info.granted) {
                Spacer(Modifier.height(10.dp))
                RestrictedSettingsFix()
            }
        }
    }
}

@Composable
private fun StatusButton(granted: Boolean, onGrant: () -> Unit) {
    val view = LocalView.current
    if (granted) {
        Box(
            Modifier
                .background(OffipeColors.Success.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OffipeColors.Success,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "GRANTED",
                    style = OffipeType.LabelSmall,
                    color = OffipeColors.Success
                )
            }
        }
    } else {
        Box(
            Modifier
                .background(OffipeColors.Accent)
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onGrant()
                }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "GRANT",
                style = OffipeType.LabelSmall,
                color = OffipeColors.Black
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
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = OffipeColors.Accent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (expanded) "Hide fix ▲" else "Can't enable? Tap for fix ▼",
                style = OffipeType.LabelSmall,
                color = OffipeColors.Accent
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                Modifier
                    .padding(top = 8.dp)
                    .background(OffipeColors.Surface)
                    .padding(14.dp)
            ) {
                // Explanation
                Text(
                    text = "Why does this happen?",
                    style = OffipeType.LabelSmall,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Since Offipe is sideloaded (not from Play Store), Android 13+ blocks restricted settings like Accessibility by default. This is the same reason you had to disable Play Protect — it's a mandatory Google security measure for sideloaded apps.",
                    style = OffipeType.BodySmall,
                    color = OffipeColors.TextMuted
                )
                Spacer(Modifier.height(12.dp))

                // Steps
                Text(
                    text = "FIX:",
                    style = OffipeType.LabelSmall,
                    color = OffipeColors.Accent
                )
                Spacer(Modifier.height(8.dp))
                NumberedStep(1, "Go to your phone's Settings → Apps → Offipe")
                Spacer(Modifier.height(6.dp))
                NumberedStep(2, "Tap the ⋮ three dots in the top right corner")
                Spacer(Modifier.height(6.dp))
                NumberedStep(3, "Select \"Allow restricted settings\" and confirm with your PIN/fingerprint")
                Spacer(Modifier.height(6.dp))
                NumberedStep(4, "Now open Offipe and enable the Accessibility service — it will work cleanly")

                Spacer(Modifier.height(14.dp))

                // Guide link
                OffipeSecondaryButton(
                    text = "View Guide with Screenshots",
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://cleanbrowsing.org/support/mobile/disable-restricted-settings-android")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Page 6: Ready ──

@Composable
private fun ReadyPage() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(96.dp)
                .background(OffipeColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = OffipeColors.Black,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "YOU'RE SET",
            style = OffipeType.LabelMedium,
            color = OffipeColors.Accent
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Time to pay.",
            style = OffipeType.DisplayLarge,
            color = OffipeColors.TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}


