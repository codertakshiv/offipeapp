package com.offipe.app.presentation.screens

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.offipe.app.R
import com.offipe.app.presentation.permissions.openAccessibilitySettings
import com.offipe.app.presentation.ui.components.NeoPopCard
import com.offipe.app.presentation.ui.components.NeoPopPrimaryButton
import com.offipe.app.presentation.ui.components.NeoPopSecondaryButton
import com.offipe.app.presentation.ui.theme.NeoPopColors
import com.offipe.app.presentation.ui.theme.NeoPopType

// ─── Hooks for the user to drop in real assets later ──────────────────────────

private val step1ImageRes: Int? = R.drawable.bhim_step1
private val step2ImageRes: Int? = R.drawable.bhim_step2
private val step3ImageRes: Int? = R.drawable.bhim_step3

private val TUTORIAL_VIDEO_URL: String? =
    "https://youtube.com/playlist?list=PL6zhuU_l94t1y25MDt96Z-MltD3S6iPFj&si=GNlanTwR-IcfOBI"

private const val GITHUB_REPO_URL = "https://github.com/laksh-ya/OffipeApp/"
private const val RESTRICTED_SETTINGS_GUIDE_URL =
    "https://cleanbrowsing.org/support/mobile/disable-restricted-settings-android"

/**
 * Help/FAQ screen — clean, accordion-style Q&A layout.
 *
 * Each question is a tappable header that expands to reveal the answer.
 * Links (GitHub, video tutorial, replay onboarding) sit at the bottom.
 */
@Composable
fun FaqScreen(
    onClose: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
            .statusBarsPadding()
    ) {
        // ── Top bar ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CornerCloseButton(onClick = onClose)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "HELP & FAQ",
                style = OffipeType.LabelMedium,
                color = OffipeColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(40.dp))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Hero blurb ──
            val heroText = buildAnnotatedString {
                append("USSD can be confusing. here's how ")
                withStyle(SpanStyle(color = OffipeColors.Accent)) { append("Offipe") }
                append(" makes it simple.")
            }
            Text(
                text = heroText,
                style = OffipeType.DisplayMedium.copy(
                    fontSize = androidx.compose.ui.unit.TextUnit(
                        24f, androidx.compose.ui.unit.TextUnitType.Sp
                    )
                ),
                color = OffipeColors.TextPrimary
            )

            Spacer(Modifier.height(24.dp))

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ACCORDION Q&A ITEMS
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            // 1. Enable Banking (*99#) — one-time setup
            ExpandableQuestion(
                question = "How do I enable banking via *99#?",
                badge = "ONE-TIME"
            ) {
                Text(
                    text = "Before using Offipe, you need to enable your bank on the *99# USSD channel. This only needs to be done once.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                NumberedStep(1, "Dial *99# from your phone's dialer")
                Spacer(Modifier.height(6.dp))
                NumberedStep(2, "Enter your bank name when prompted (e.g. SBI, HDFC)")
                Spacer(Modifier.height(6.dp))
                NumberedStep(3, "Follow on-screen prompts to link your bank account")
                Spacer(Modifier.height(14.dp))
                NeoPopPrimaryButton(
                    text = "Dial *99#",
                    leadingIcon = Icons.Default.Phone,
                    onClick = {
                        val i = Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse("tel:*99${Uri.encode("#")}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                NeoPopSecondaryButton(
                    text = "Official *99# Guide",
                    leadingIcon = Icons.Default.PlayArrow,
                    onClick = {
                        val i = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.bhimupi.org.in/steps-to-use-99#")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. BHIM walkthrough
            ExpandableQuestion(question = "How do I enable *99# in the BHIM app?") {
                Text(
                    text = "If you use BHIM, toggle on the *99# USSD service in BHIM settings. Done once, you're set.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))

                NumberedStep(1, "Open BHIM → tap your profile avatar (top-left).")
                ImageSlot(res = step1ImageRes)
                Spacer(Modifier.height(10.dp))

                NumberedStep(2, "Scroll down → tap Settings.")
                ImageSlot(res = step2ImageRes)
                Spacer(Modifier.height(10.dp))

                NumberedStep(3, "Find \"USSD service (*99#)\" → toggle ON.")
                ImageSlot(res = step3ImageRes)
            }

            // 3. Supported carriers
            ExpandableQuestion(question = "Which carriers are supported?") {
                Text(
                    text = "Airtel, Vodafone (Vi), and BSNL — yes. Jio — no. That's a technical limitation on Jio's network, not Offipe.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarrierTag("AIRTEL", true)
                    CarrierTag("VI", true)
                    CarrierTag("BSNL", true)
                    CarrierTag("JIO", false)
                }
            }

            // 4. Accessibility
            ExpandableQuestion(question = "How do I enable the accessibility service?") {
                NumberedStep(1, "Open phone Settings → Accessibility → Installed apps.")
                Spacer(Modifier.height(6.dp))
                NumberedStep(2, "Find Offipe → toggle ON → confirm the dialog.")
                Spacer(Modifier.height(6.dp))
                NumberedStep(3, "Come back to Offipe. The tile should show ENABLED.")
                Spacer(Modifier.height(14.dp))
                NeoPopSecondaryButton(
                    text = "Open Accessibility Settings",
                    onClick = { openAccessibilitySettings(context) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Can't enable accessibility
            ExpandableQuestion(
                question = "Can't enable accessibility?",
                accentColor = OffipeColors.Danger
            ) {
                Text(
                    text = "Since Offipe is sideloaded (not from Play Store), Android 13+ blocks restricted settings by default. Same reason you had to disable Play Protect — it's a mandatory Google security measure.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "SOLUTION:",
                    style = OffipeType.LabelMedium,
                    color = OffipeColors.Accent
                )
                Spacer(Modifier.height(8.dp))
                NumberedStep(1, "Settings → Apps → Offipe")
                Spacer(Modifier.height(6.dp))
                NumberedStep(2, "Tap the ⋮ three dots (top right)")
                Spacer(Modifier.height(6.dp))
                NumberedStep(3, "\"Allow restricted settings\" → confirm with PIN", emphasised = true)
                Spacer(Modifier.height(6.dp))
                NumberedStep(4, "Open Offipe → enable Accessibility")
                Spacer(Modifier.height(14.dp))
                NeoPopSecondaryButton(
                    text = "View Guide with Screenshots",
                    leadingIcon = Icons.Default.PlayArrow,
                    onClick = {
                        val i = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(RESTRICTED_SETTINGS_GUIDE_URL)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. What is *99#
            ExpandableQuestion(question = "What is *99# UPI?") {
                Text(
                    text = "*99# is India's USSD-based UPI service. It works without internet — you dial a code, your bank shows menus through SMS-style dialogs, and you reply to send money. Works on any phone with a cellular signal.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // 7. Two modes
            ExpandableQuestion(question = "What are the two modes?") {
                ModeRow(
                    name = "Auto",
                    desc = "Default. Offipe handles the carrier dialog automatically — you stay in the app throughout."
                )
                Spacer(Modifier.height(10.dp))
                ModeRow(
                    name = "Manual",
                    desc = "Copies the UPI ID and opens the dialer. You complete the rest yourself. No accessibility needed."
                )
            }

            // 8. Overlay
            ExpandableQuestion(question = "How does the overlay work?") {
                Text(
                    text = "Offipe drives the carrier's USSD dialog automatically using Android's accessibility service. We never see your PIN — it's typed locally on your device, never stored, and never sent to any server.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // 9. PIN safety
            ExpandableQuestion(question = "Is my PIN safe?") {
                Text(
                    text = "Yes. Your PIN stays in volatile memory only, masked as •••• in logs, cleared within 500ms of session end, never stored, and never shared.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // 10. Failures
            ExpandableQuestion(question = "What if it fails?") {
                Text(
                    text = "We show the carrier's exact error message. If your bank isn't linked to *99#, we direct you to setup instructions. A 25-second timeout prevents stuck sessions.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // 11. Network Security Risks
            ExpandableQuestion(question = "What are the network security risks of USSD?") {
                Text(
                    text = "USSD (*99#) signaling occurs over the unencrypted GSM voice channel, meaning it is not encrypted end-to-end like HTTPS. This makes it theoretically vulnerable to carrier-level interception, SIM swapping, and base station spoofing (IMSI catchers). Offipe has no control over cellular protocol security; users should operate with normal cellular safety precautions.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // 12. Clipboard Hijacking
            ExpandableQuestion(question = "Can other apps steal details from the clipboard?") {
                Text(
                    text = "In Manual Mode, Offipe copies details (like the recipient's UPI ID) to the system clipboard. If your device has malicious applications installed, they may perform clipboard hijacking to read or alter copied text. Always verify the recipient's name on the carrier's final confirmation screen before entering your PIN.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // 13. Screen Recording / Casting Risks
            ExpandableQuestion(question = "Is it safe to enter my PIN during screen share?") {
                Text(
                    text = "No. Since Offipe works entirely offline, it cannot monitor or block active screen recording, casting, or remote-desktop apps (AnyDesk, Zoom, TeamViewer). If you enter your PIN while sharing your screen, your PIN could be visually compromised. Ensure all sharing/recording is stopped before initiating a payment.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // 14. Custom Android OEM Skins
            ExpandableQuestion(question = "Why does automation fail on custom Android skins?") {
                Text(
                    text = "Custom Android distributions (like Xiaomi's HyperOS/MIUI, OPPO's ColorOS, or Vivo's Funtouch OS) modify the layout structure of system USSD dialogs. This can prevent the Accessibility Service from parsing the menus correctly in Auto Mode. If you experience issues, switch to Manual Mode in the app Settings to execute the transaction safely.",
                    style = OffipeType.BodyMedium,
                    color = OffipeColors.TextSecondary
                )
            }

            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // LINKS & ACTIONS
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OffipeColors.Border)
            )
            Spacer(Modifier.height(20.dp))

            // Video tutorial
            val videoUrl = TUTORIAL_VIDEO_URL
            if (videoUrl != null) {
                NeoPopSecondaryButton(
                    text = "Watch Video Tutorial",
                    leadingIcon = Icons.Default.PlayArrow,
                    onClick = {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            // GitHub
            NeoPopSecondaryButton(
                text = "Source Code & Guides (GitHub)",
                leadingIcon = Icons.Default.Code,
                onClick = {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(i) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Additional video guides, releases & source code are available on the GitHub repo.",
                style = OffipeType.BodySmall,
                color = OffipeColors.TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Replay onboarding
            NeoPopSecondaryButton(
                text = "Replay Onboarding",
                leadingIcon = Icons.Default.Refresh,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onReplayOnboarding()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            CookieEasterEgg()

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── Expandable Q&A item ─────────────────────────────────────────────────────

/**
 * Accordion-style question/answer row. Tapping the question toggles the
 * answer visibility. An optional [badge] (e.g. "ONE-TIME") sits next to the
 * question text.
 */
@Composable
private fun ExpandableQuestion(
    question: String,
    badge: String? = null,
    accentColor: androidx.compose.ui.graphics.Color = OffipeColors.Accent,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "qa_arrow"
    )

    NeoPopCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Question header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        expanded = !expanded
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = OffipeType.TitleMedium,
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .background(accentColor.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = OffipeType.LabelSmall,
                            color = accentColor
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = OffipeColors.TextMuted,
                    modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                )
            }

            // Answer body
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
}

// ─── Small re-usable bits ────────────────────────────────────────────────────

@Composable
private fun ModeRow(name: String, desc: String) {
    Column {
        Text(name, style = OffipeType.TitleMedium, color = OffipeColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(desc, style = OffipeType.BodyMedium, color = OffipeColors.TextSecondary)
    }
}

@Composable
private fun CarrierTag(name: String, supported: Boolean) {
    val c = if (supported) OffipeColors.Success else OffipeColors.Danger
    Box(
        Modifier
            .background(c.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(name, style = OffipeType.LabelSmall, color = c)
    }
}

@Composable
private fun NumberedStep(index: Int, body: String, emphasised: Boolean = false) {
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
            color = if (emphasised) OffipeColors.TextPrimary else OffipeColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ImageSlot(res: Int?) {
    Spacer(Modifier.height(10.dp))
    if (res != null) {
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
            Text("step image", style = OffipeType.LabelMedium, color = OffipeColors.TextMuted)
        }
    }
}

@Composable
private fun CornerCloseButton(onClick: () -> Unit) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "close_scale"
    )
    Box(
        Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                drawRect(
                    color = OffipeColors.TextPrimary.copy(alpha = 0.12f),
                    style = Stroke(width = 1f)
                )
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = OffipeColors.TextPrimary
        )
    }
}

@Composable
private fun CookieEasterEgg() {
    val view = LocalView.current
    var taken by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (taken) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cookie_scale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (taken) 0f else -25f,
        animationSpec = tween(durationMillis = 600),
        label = "cookie_rot"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .clickable {
                    view.performHapticFeedback(
                        if (!taken) HapticFeedbackConstants.CONFIRM
                        else HapticFeedbackConstants.VIRTUAL_KEY
                    )
                    taken = !taken
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (taken) "(tap to put it back)" else "here's a cookie for you :)",
                style = OffipeType.LabelMedium,
                color = OffipeColors.Accent
            )
        }
        AnimatedVisibility(visible = taken, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
            ) {
                Text(
                    text = "🍪",
                    fontSize = androidx.compose.ui.unit.TextUnit(
                        96f, androidx.compose.ui.unit.TextUnitType.Sp
                    )
                )
            }
        }
    }
}

