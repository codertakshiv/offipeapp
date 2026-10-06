package com.offipe.app.presentation.screens

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.offipe.app.R
import com.offipe.app.data.PreferencesRepository
import com.offipe.app.domain.OperationMode
import com.offipe.app.presentation.HistoryViewModel
import com.offipe.app.presentation.permissions.PermissionStatus
import com.offipe.app.presentation.permissions.openAccessibilitySettings
import com.offipe.app.presentation.permissions.openOverlaySettings
import com.offipe.app.presentation.permissions.rememberPermissionLaunchers
import com.offipe.app.presentation.ui.components.Chip
import com.offipe.app.presentation.ui.components.Hairline
import com.offipe.app.presentation.ui.components.LedDot
import com.offipe.app.presentation.ui.components.OffipeDialog
import com.offipe.app.presentation.ui.components.ReadoutRow
import com.offipe.app.presentation.ui.components.Segmented
import com.offipe.app.presentation.ui.components.Tag
import com.offipe.app.presentation.ui.components.TextKey
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import com.offipe.app.presentation.ui.components.offipeLogoPainter
import kotlinx.coroutines.launch

/**
 * SETTINGS — the reference's grouped-row control center.
 *
 * Two labelled groups, hairline-separated rows, no cards:
 *  - Payment: Payment Mode and UPI PIN Length expand in place (segmented
 *    control + live description); Permissions expands into the four
 *    permission rows with Grant keys, the sideload troubleshoot and an
 *    "All granted" green check when everything is on.
 *  - General: Transaction History, Help & FAQ, Privacy Policy,
 *    Terms of Use, Clear All Data (danger confirm), About.
 *
 * Tapping the About row still summons the cat overlay easter egg.
 */
@Composable
fun SettingsScreen(
    prefsRepo: PreferencesRepository,
    historyViewModel: HistoryViewModel,
    permissions: PermissionStatus,
    versionName: String,
    onClearAllData: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode by prefsRepo.operationMode.collectAsState(initial = OperationMode.AUTO)
    val pinLength by prefsRepo.pinLength.collectAsState(initial = 6)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val launchers = rememberPermissionLaunchers()
    val view = LocalView.current

    val allGranted = permissions.phoneBundle && permissions.camera &&
        permissions.accessibility && permissions.overlay
    var modeOpen by remember { mutableStateOf(false) }
    var pinOpen by remember { mutableStateOf(false) }
    var accessOpen by remember { mutableStateOf(!allGranted) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Title ──
        Text(
            text = "Settings",
            style = OffipeType.DisplayMedium,
            color = OffipeColors.TextPrimary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
        )
        Text(
            text = "Offline UPI · *99#  ·  v$versionName",
            style = OffipeType.TerminalLabel,
            color = OffipeColors.TextMuted,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(20.dp))

        // ── Payment ──
        GroupLabel("Payment")
        Column(Modifier.padding(horizontal = 20.dp)) {
            ReadoutRow(
                label = "Payment Mode",
                value = "",
                sublabel = if (mode == OperationMode.AUTO) "Auto (Recommended)" else "Manual",
                leading = { RowIcon(Icons.Default.ChevronRight, tint = OffipeColors.TextMuted) },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    modeOpen = !modeOpen
                    pinOpen = false
                },
                trailing = { Chevron(modeOpen) }
            )
            AnimatedVisibility(visible = modeOpen) {
                Column {
                    Segmented(
                        options = listOf("Auto", "Manual"),
                        selectedIndex = if (mode == OperationMode.MANUAL) 1 else 0,
                        onSelect = { i ->
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            scope.launch {
                                prefsRepo.setOperationMode(
                                    if (i == 1) OperationMode.MANUAL else OperationMode.AUTO
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        text = if (mode == OperationMode.MANUAL) {
                            "Copies the UPI ID and opens the dialer — you complete the rest. " +
                                "No accessibility needed."
                        } else {
                            "Offipe handles the carrier dialog automatically — you stay in the app."
                        },
                        style = OffipeType.BodySmall,
                        color = OffipeColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
            Hairline()

            ReadoutRow(
                label = "UPI PIN Length",
                value = "",
                sublabel = "$pinLength Digits",
                leading = { RowIcon(Icons.Default.ChevronRight, tint = OffipeColors.TextMuted) },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    pinOpen = !pinOpen
                    modeOpen = false
                },
                trailing = { Chevron(pinOpen) }
            )
            AnimatedVisibility(visible = pinOpen) {
                Segmented(
                    options = listOf("4 Digits", "6 Digits"),
                    selectedIndex = if (pinLength == 4) 0 else 1,
                    onSelect = { i ->
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        scope.launch { prefsRepo.setPinLength(if (i == 0) 4 else 6) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
            Hairline()

            ReadoutRow(
                label = "Permissions",
                value = "",
                sublabel = if (allGranted) "All granted" else "Action needed",
                leading = {
                    if (allGranted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OffipeColors.Success,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        RowIcon(Icons.Default.ChevronRight, tint = OffipeColors.TextMuted)
                    }
                },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    accessOpen = !accessOpen
                    modeOpen = false
                    pinOpen = false
                },
                trailing = { Chevron(accessOpen) }
            )
            AnimatedVisibility(visible = accessOpen) {
                Column {
                    PermissionRow(
                        label = "Phone access",
                        detail = "Dial *99# and read SIM info",
                        granted = permissions.phoneBundle,
                        onFix = { launchers.requestPhoneBundle() }
                    )
                    PermissionRow(
                        label = "Camera",
                        detail = "Scan UPI QR codes",
                        granted = permissions.camera,
                        onFix = { launchers.requestCamera() }
                    )
                    PermissionRow(
                        label = "Accessibility",
                        detail = "Read and respond to the carrier dialog",
                        granted = permissions.accessibility,
                        onFix = { openAccessibilitySettings(context) }
                    )
                    PermissionRow(
                        label = "Display over apps",
                        detail = "Cover the system USSD dialog",
                        granted = permissions.overlay,
                        onFix = { openOverlaySettings(context) }
                    )

                    if (!permissions.accessibility) {
                        Spacer(Modifier.height(6.dp))
                        AccessibilityTroubleshoot()
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── General ──
        GroupLabel("General")
        Column(Modifier.padding(horizontal = 20.dp)) {
            LinkRow("Transaction History", onOpenHistory)
            Hairline()
            LinkRow("Help & FAQ", onOpenFaq)
            Hairline()
            LinkRow("Privacy Policy", onOpenPrivacy)
            Hairline()
            LinkRow("Terms of Use", onOpenTerms)
            Hairline()
            ReadoutRow(
                label = "Clear All Data",
                value = "",
                valueColor = OffipeColors.Danger,
                sublabel = "Delete every stored transaction",
                onClick = { confirmClear = true },
                trailing = { Chip(text = "Clear", color = OffipeColors.Danger) }
            )
            Hairline()
            AboutRow(versionName = versionName)
        }

        Spacer(Modifier.height(36.dp))
    }

    if (confirmClear) {
        OffipeDialog(
            title = "Clear transaction history?",
            message = "Every locally-stored payment record will be deleted. " +
                "This cannot be undone.",
            confirmLabel = "Clear",
            onConfirm = {
                confirmClear = false
                onClearAllData()
            },
            onDismiss = { confirmClear = false },
            danger = true
        )
    }
}

// ─── Primitives ───────────────────────────────────────────────────────────────

@Composable
private fun GroupLabel(label: String) {
    Text(
        text = label,
        style = OffipeType.LabelMedium,
        color = OffipeColors.TextSecondary,
        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun RowIcon(icon: ImageVector, tint: Color = OffipeColors.TextPrimary) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(OffipeColors.SurfaceHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun Chevron(expanded: Boolean) {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = OffipeColors.TextMuted,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = if (expanded) 90f else 0f }
    )
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    val view = LocalView.current
    ReadoutRow(
        label = label,
        value = "",
        leading = { RowIcon(Icons.Default.ChevronRight, tint = OffipeColors.TextMuted) },
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        },
        trailing = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OffipeColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun PermissionRow(
    label: String,
    detail: String,
    granted: Boolean,
    onFix: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedDot(
            size = 6.dp,
            color = if (granted) OffipeColors.Success else OffipeColors.Warn,
            pulse = !granted
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = OffipeType.LabelMedium,
                color = OffipeColors.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = OffipeType.BodySmall,
                color = OffipeColors.TextMuted
            )
        }
        Spacer(Modifier.width(10.dp))
        if (granted) {
            Tag("Granted", color = OffipeColors.Success)
        } else {
            TextKey(text = "Grant", onClick = onFix, color = OffipeColors.Mark)
        }
    }
}

/**
 * Sideload accessibility troubleshoot — inline notice with numbered
 * steps and two recovery keys.
 */
@Composable
private fun AccessibilityTroubleshoot() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(OffipeColors.SurfaceHigh)
            .padding(14.dp)
    ) {
        Tag("Unable to enable accessibility?", color = OffipeColors.Warn)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Since Offipe is sideloaded (not from Play Store), Android 13+ " +
                "blocks restricted settings by default — the same security measure " +
                "behind disabling Play Protect.",
            style = OffipeType.BodySmall,
            color = OffipeColors.TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Tag("Solution", color = OffipeColors.TextSecondary)
        Spacer(Modifier.height(7.dp))
        StepRow(1, "Go to Settings → Apps → Offipe")
        StepRow(2, "Tap the ⋮ three dots (top right)")
        StepRow(3, "Select \"Allow restricted settings\" → confirm with PIN")
        StepRow(4, "Open Offipe → enable Accessibility service")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextKey(
                text = "Open settings",
                onClick = { openAccessibilitySettings(context) },
                color = OffipeColors.Mark
            )
            Spacer(Modifier.width(10.dp))
            TextKey(
                text = "View guide",
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://cleanbrowsing.org/support/mobile/" +
                                "disable-restricted-settings-android"
                        )
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                },
                color = OffipeColors.TextSecondary
            )
        }
    }
}

@Composable
private fun StepRow(index: Int, text: String) {
    Row(
        Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            style = OffipeType.TerminalLabel,
            color = OffipeColors.TextMuted,
            modifier = Modifier.width(22.dp)
        )
        Text(
            text = text,
            style = OffipeType.BodySmall,
            color = OffipeColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * About row — mark, name, version.
 */
@Composable
private fun AboutRow(versionName: String) {
    val view = LocalView.current

    ReadoutRow(
        label = "About",
        value = "",
        sublabel = "Offipe · V$versionName",
        leading = {
            Image(
                painter = offipeLogoPainter(),
                contentDescription = "Offipe",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(34.dp)
            )
        },
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        },
        trailing = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OffipeColors.TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}
