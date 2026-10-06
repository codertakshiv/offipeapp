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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offipe.app.presentation.permissions.openAccessibilitySettings
import com.offipe.app.presentation.ui.components.Chip
import com.offipe.app.presentation.ui.components.GhostActionBar
import com.offipe.app.presentation.ui.components.Hairline
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.LedDot
import com.offipe.app.presentation.ui.components.PrimaryActionBar
import com.offipe.app.presentation.ui.components.Tag
import com.offipe.app.presentation.ui.components.TextKey
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType

// ─── Hooks for the user to drop in real assets later ──────────────────────────

private val TUTORIAL_VIDEO_URL: String? =
    "https://youtube.com/playlist?list=PL6zhuU_l94t1y25MDt96Z-MltD3S6iPFj&si=GNlanTwR-IcfOBI"

private const val GITHUB_REPO_URL = "https://github.com/laksh-ya/OffipeApp/"
private const val RESTRICTED_SETTINGS_GUIDE_URL =
    "https://cleanbrowsing.org/support/mobile/disable-restricted-settings-android"

/**
 * FAQ — the indexed manual.
 *
 * Numbered hairline accordion: five topic groups, each question a ruled
 * row that expands into body prose, mono-numbered steps and full-bleed
 * action bars. Header strip carries ESC plus the replay-intro key.
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
            .background(OffipeColors.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconKey(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onClose,
                    size = 38.dp,
                    tint = OffipeColors.TextSecondary
                )
                Spacer(Modifier.width(12.dp))
                Tag("FAQ · Help", color = OffipeColors.TextSecondary)
                Spacer(Modifier.weight(1f))
                TextKey(
                    text = "Replay intro",
                    color = OffipeColors.Mark,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onReplayOnboarding()
                    }
                )
            }
            Hairline()
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
        ) {
            val heroText = buildAnnotatedString {
                append("USSD can be confusing. here's how ")
                withStyle(SpanStyle(color = OffipeColors.Accent)) { append("Offipe") }
                append(" makes it simple.")
            }
            Text(
                text = heroText,
                style = OffipeType.DisplayMedium,
                color = OffipeColors.TextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp)
            )

            FaqTopicSection(tag = "01", title = "Getting started", count = 2) {
                ExpandableQuestion(
                    question = "How do I enable banking via *99#?",
                    badge = "ONE-TIME"
                ) {
                    Prose(
                        "Before using Offipe, you need to enable your bank on the " +
                            "*99# USSD channel. This only needs to be done once."
                    )
                    Spacer(Modifier.height(12.dp))
                    NumberedStep(1, "Dial *99# from your phone's dialer")
                    NumberedStep(2, "Enter your bank name when prompted (e.g. SBI, HDFC)")
                    NumberedStep(3, "Follow on-screen prompts to link your bank account")
                    Spacer(Modifier.height(14.dp))
                    PrimaryActionBar(
                        text = "Dial *99#",
                        icon = Icons.Default.Phone,
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
                    GhostActionBar(
                        text = "Official *99# Guide",
                        icon = Icons.Default.PlayArrow,
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

                ExpandableQuestion(question = "Which carriers are supported?") {
                    Prose(
                        "Airtel, Vodafone (Vi), and BSNL — yes. Jio — no. That's a " +
                            "technical limitation on Jio's network, not Offipe."
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CarrierTag("AIRTEL", true)
                        CarrierTag("VI", true)
                        CarrierTag("BSNL", true)
                        CarrierTag("JIO", false)
                    }
                }
            }

            FaqTopicSection(tag = "02", title = "Permissions", count = 2) {
                ExpandableQuestion(question = "How do I enable the accessibility service?") {
                    Spacer(Modifier.height(6.dp))
                    NumberedStep(1, "Open phone Settings → Accessibility → Installed apps.")
                    NumberedStep(2, "Find Offipe → toggle ON → confirm the dialog.")
                    NumberedStep(3, "Come back to Offipe. The tile should show ENABLED.")
                    Spacer(Modifier.height(14.dp))
                    GhostActionBar(
                        text = "Open Accessibility Settings",
                        onClick = { openAccessibilitySettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ExpandableQuestion(
                    question = "Can't enable accessibility?",
                    accentColor = OffipeColors.Danger
                ) {
                    Prose(
                        "Since Offipe is sideloaded (not from Play Store), Android 13+ " +
                            "blocks restricted settings by default. Same reason you had to " +
                            "disable Play Protect — it's a mandatory Google security measure."
                    )
                    Spacer(Modifier.height(14.dp))
                    Tag(
                        "Solution",
                        color = OffipeColors.Signal,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    NumberedStep(1, "Settings → Apps → Offipe")
                    NumberedStep(2, "Tap the ⋮ three dots (top right)")
                    NumberedStep(3, "\"Allow restricted settings\" → confirm with PIN", emphasised = true)
                    NumberedStep(4, "Open Offipe → enable Accessibility")
                    Spacer(Modifier.height(14.dp))
                    GhostActionBar(
                        text = "View Guide with Screenshots",
                        icon = Icons.Default.PlayArrow,
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
            }

            FaqTopicSection(tag = "03", title = "How it works", count = 3) {
                ExpandableQuestion(question = "What is *99# UPI?") {
                    Prose(
                        "*99# is India's USSD-based UPI service. It works without " +
                            "internet — you dial a code, your bank shows menus through " +
                            "SMS-style dialogs, and you reply to send money. Works on any " +
                            "phone with a cellular signal."
                    )
                }

                ExpandableQuestion(question = "What are the two modes?") {
                    ModeRow(
                        name = "Auto",
                        desc = "Default. Offipe handles the carrier dialog automatically — " +
                            "you stay in the app throughout.",
                        live = true
                    )
                    Hairline()
                    ModeRow(
                        name = "Manual",
                        desc = "Copies the UPI ID and opens the dialer. You complete the " +
                            "rest yourself. No accessibility needed."
                    )
                }

                ExpandableQuestion(question = "How does the overlay work?") {
                    Prose(
                        "Offipe drives the carrier's USSD dialog automatically using " +
                            "Android's accessibility service. We never see your PIN — it's " +
                            "typed locally on your device, never stored, and never sent to " +
                            "any server."
                    )
                }
            }

            FaqTopicSection(tag = "04", title = "Security", count = 4) {
                ExpandableQuestion(question = "Is my PIN safe?") {
                    Prose(
                        "Yes. Your PIN stays in volatile memory only, masked as •••• in " +
                            "logs, cleared within 500ms of session end, never stored, and " +
                            "never shared."
                    )
                }

                ExpandableQuestion(question = "What are the network security risks of USSD?") {
                    Prose(
                        "USSD (*99#) signaling occurs over the unencrypted GSM voice " +
                            "channel, meaning it is not encrypted end-to-end like HTTPS. " +
                            "This makes it theoretically vulnerable to carrier-level " +
                            "interception, SIM swapping, and base station spoofing (IMSI " +
                            "catchers). Offipe has no control over cellular protocol " +
                            "security; users should operate with normal cellular safety " +
                            "precautions."
                    )
                }

                ExpandableQuestion(question = "Can other apps steal details from the clipboard?") {
                    Prose(
                        "In Manual Mode, Offipe copies details (like the recipient's UPI " +
                            "ID) to the system clipboard. If your device has malicious " +
                            "applications installed, they may perform clipboard hijacking " +
                            "to read or alter copied text. Always verify the recipient's " +
                            "name on the carrier's final confirmation screen before " +
                            "entering your PIN."
                    )
                }

                ExpandableQuestion(question = "Is it safe to enter my PIN during screen share?") {
                    Prose(
                        "No. Since Offipe works entirely offline, it cannot monitor or " +
                            "block active screen recording, casting, or remote-desktop " +
                            "apps (AnyDesk, Zoom, TeamViewer). If you enter your PIN while " +
                            "sharing your screen, your PIN could be visually compromised. " +
                            "Ensure all sharing/recording is stopped before initiating a " +
                            "payment."
                    )
                }
            }

            FaqTopicSection(tag = "05", title = "Troubleshooting", count = 2) {
                ExpandableQuestion(question = "What if it fails?") {
                    Prose(
                        "We show the carrier's exact error message. If your bank isn't " +
                            "linked to *99#, we direct you to setup instructions. A " +
                            "25-second timeout prevents stuck sessions."
                    )
                }

                ExpandableQuestion(question = "Why does automation fail on custom Android skins?") {
                    Prose(
                        "Custom Android distributions (like Xiaomi's HyperOS/MIUI, " +
                            "OPPO's ColorOS, or Vivo's Funtouch OS) modify the layout " +
                            "structure of system USSD dialogs. This can prevent the " +
                            "Accessibility Service from parsing the menus correctly in " +
                            "Auto Mode. If you experience issues, switch to Manual Mode " +
                            "in the app Settings to execute the transaction safely."
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            Hairline()

            val videoUrl = TUTORIAL_VIDEO_URL
            if (videoUrl != null) {
                GhostActionBar(
                    text = "Watch Video Tutorial",
                    icon = Icons.Default.PlayArrow,
                    onClick = {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            GhostActionBar(
                text = "Source Code & Guides (GitHub)",
                icon = Icons.Default.Code,
                onClick = {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(i) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Additional video guides, releases & source code are available " +
                    "on the GitHub repo.",
                style = OffipeType.BodySmall,
                color = OffipeColors.TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            GhostActionBar(
                text = "Replay Onboarding",
                icon = Icons.Default.Refresh,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onReplayOnboarding()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            CookieEasterEgg()
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─── Accordion structure ─────────────────────────────────────────────────────

@Composable
private fun FaqTopicSection(
    tag: String,
    title: String,
    count: Int,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "topic_arrow"
    )

    Column(Modifier.fillMaxWidth()) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    expanded = !expanded
                }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                style = OffipeType.DisplaySmall,
                color = if (expanded) OffipeColors.Mark else OffipeColors.TextMuted
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OffipeType.HeadlineLarge,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(5.dp))
                Tag(
                    "$count questions",
                    led = if (expanded) OffipeColors.Mark else null,
                    color = OffipeColors.TextMuted
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = if (expanded) OffipeColors.Mark else OffipeColors.TextMuted,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun ExpandableQuestion(
    question: String,
    badge: String? = null,
    accentColor: Color = OffipeColors.TextPrimary,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "qa_arrow"
    )

    Column(Modifier.fillMaxWidth()) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    expanded = !expanded
                }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                style = OffipeType.TitleMedium,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Spacer(Modifier.width(10.dp))
                Chip(text = badge, color = accentColor)
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = OffipeColors.TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = arrowRotation }
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 18.dp)
            ) {
                content()
            }
        }
    }
}

// ─── Small re-usable bits ────────────────────────────────────────────────────

@Composable
private fun Prose(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = OffipeType.BodyMedium,
        color = OffipeColors.TextSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

@Composable
private fun ModeRow(name: String, desc: String, live: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        LedDot(
            size = 5.dp,
            color = if (live) OffipeColors.Mark else OffipeColors.TextMuted,
            pulse = live,
            modifier = Modifier.padding(top = 7.dp)
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = OffipeType.TitleLarge, color = OffipeColors.TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(desc, style = OffipeType.BodyMedium, color = OffipeColors.TextSecondary)
        }
    }
}

@Composable
private fun CarrierTag(name: String, supported: Boolean) {
    Chip(
        text = name,
        color = if (supported) OffipeColors.Success else OffipeColors.Danger
    )
}

@Composable
private fun NumberedStep(index: Int, body: String, emphasised: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            style = OffipeType.Mono,
            color = if (emphasised) OffipeColors.Mark else OffipeColors.TextMuted,
            modifier = Modifier.width(30.dp)
        )
        Text(
            text = body,
            style = OffipeType.BodyMedium,
            color = if (emphasised) OffipeColors.TextPrimary else OffipeColors.TextSecondary,
            modifier = Modifier.weight(1f)
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
                Text(text = "🍪", fontSize = 96.sp)
            }
        }
    }
}
