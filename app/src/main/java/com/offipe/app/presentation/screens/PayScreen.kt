package com.offipe.app.presentation.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offipe.app.R
import com.offipe.app.domain.FormField
import com.offipe.app.domain.MobileLookupParser
import com.offipe.app.domain.OperationMode
import com.offipe.app.domain.SessionState
import com.offipe.app.presentation.MobileLookupState
import com.offipe.app.presentation.PayUiState
import com.offipe.app.presentation.PayMode
import com.offipe.app.presentation.PayViewModel
import com.offipe.app.presentation.permissions.PermissionStatus
import com.offipe.app.presentation.permissions.openAccessibilitySettings
import com.offipe.app.presentation.permissions.openOverlaySettings
import com.offipe.app.presentation.permissions.rememberPermissionLaunchers
import com.offipe.app.presentation.ui.components.AlertStrip
import com.offipe.app.presentation.ui.components.AlertTone
import com.offipe.app.presentation.ui.components.IconKey
import com.offipe.app.presentation.ui.components.InlineField
import com.offipe.app.presentation.ui.components.NumericPad
import com.offipe.app.presentation.ui.components.OffipeDialog
import com.offipe.app.presentation.ui.components.PanelCard
import com.offipe.app.presentation.ui.components.PinGate
import com.offipe.app.presentation.ui.components.PrimaryActionBar
import com.offipe.app.presentation.ui.components.Segmented
import com.offipe.app.presentation.ui.components.SnackbarStrip
import com.offipe.app.presentation.ui.components.Tag
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import kotlinx.coroutines.delay

/**
 * Amount mask used on EVERY keypad tap. Kept here as a single compiled
 * instance so a keypress never pays for a regex compile.
 */
private val AMOUNT_ENTRY_PATTERN = Regex("^\\d{0,4}(\\.\\d{0,2})?$")

/**
 * PAY — the offline payment terminal.
 *
 * Composition, top to bottom (per the reference):
 *  1. Title block: "Pay" + `OFFLINE UPI · *99#` overline; history/help
 *     keys sit at the top right.
 *  2. Permission gates as inline notice cards (only when missing).
 *  3. DESTINATION: rounded recipient field with an inline scan key.
 *  4. AMOUNT: bold tabular readout driven by the pad below (no IME).
 *  5. NOTE: rounded optional field with an inline clear key.
 *  6. The numeric pad — rounded keys, full-cell hitboxes.
 *  7. A docked white pill (NEXT / OPEN DIALER) carrying the amount.
 *
 * Submit opens the full-screen [PinGate]; auto-fire (250ms debounce) runs
 * `attemptPayment()` the moment the PIN reaches its configured length —
 * same semantics as before, presented as an authorization takeover.
 * Session Running/Success/Failed states are rendered by the scaffold's
 * [com.offipe.app.presentation.ui.components.SessionOverlay].
 */
@Composable
fun PayScreen(
    viewModel: PayViewModel,
    onNavigateScan: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateFaq: () -> Unit,
    permissions: State<PermissionStatus>,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.uiState.collectAsState()
    val session by viewModel.sessionState.collectAsState()
    val mode by viewModel.operationMode.collectAsState()
    val snackbar by viewModel.snackbar.collectAsState()
    val pinLength by viewModel.pinLength.collectAsState(initial = 6)
    val permissionStatus = permissions.value
    var showLookupResultDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ui.lookupState) {
        showLookupResultDialog = when (ui.lookupState) {
            MobileLookupState.Idle, MobileLookupState.Looking -> false
            is MobileLookupState.Linked,
            MobileLookupState.NotLinked,
            is MobileLookupState.Failed -> true
        }
    }

    // The PIN gate opens only on submit — never while typing the form.
    var gateOpen by remember { mutableStateOf(false) }

    // Any live session takes over at the scaffold level; drop the gate so
    // it doesn't sit underneath the overlay when the session resolves.
    LaunchedEffect(session) {
        if (session !is SessionState.Idle) gateOpen = false
    }

    // Auto-fire when PIN digits entered and gate is open (debounced 250ms).
    LaunchedEffect(ui.pin, gateOpen, pinLength, session) {
        if (gateOpen && session is SessionState.Idle && ui.pin.length == pinLength) {
            delay(250)
            viewModel.attemptPayment()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
    ) {
        PayForm(
            ui = ui,
            mode = mode,
            permissions = permissionStatus,
            onPay = { viewModel.attemptPayment() },
            onScan = onNavigateScan,
            onHistory = onNavigateHistory,
            onHelp = onNavigateFaq,
            onPayModeChanged = viewModel::onModeChanged,
            onMobileChanged = viewModel::onMobileChanged,
            onFetchName = viewModel::fetchRecipientName,
            onVpa = { v -> viewModel.onFormFieldChanged(vpa = v) },
            onAmount = { v -> viewModel.onFormFieldChanged(amount = v) },
            onNote = { v -> viewModel.onFormFieldChanged(note = v) },
            onOpenGate = {
                viewModel.onPinChanged("") // clear stale digits before entry
                gateOpen = true
            },
            pinLength = pinLength
        )

        if (showLookupResultDialog) {
            when (val lookup = ui.lookupState) {
                is MobileLookupState.Linked -> OffipeDialog(
                    title = stringResource(
                        R.string.mobile_lookup_linked_title,
                        MobileLookupParser.titleCaseName(lookup.name)
                    ),
                    message = stringResource(R.string.mobile_lookup_linked_message),
                    confirmLabel = stringResource(R.string.mobile_lookup_ok),
                    onConfirm = { showLookupResultDialog = false },
                    dismissLabel = stringResource(R.string.mobile_lookup_change_number),
                    onDismiss = {
                        showLookupResultDialog = false
                        viewModel.onMobileChanged("")
                    }
                )
                MobileLookupState.NotLinked -> OffipeDialog(
                    title = stringResource(R.string.mobile_lookup_not_linked_title),
                    message = stringResource(R.string.mobile_lookup_not_linked_message),
                    confirmLabel = stringResource(R.string.mobile_lookup_use_upi_id),
                    onConfirm = {
                        showLookupResultDialog = false
                        viewModel.onModeChanged(PayMode.UPI_ID)
                    },
                    dismissLabel = stringResource(R.string.mobile_lookup_cancel),
                    onDismiss = { showLookupResultDialog = false }
                )
                is MobileLookupState.Failed -> OffipeDialog(
                    title = stringResource(R.string.mobile_lookup_failed_title),
                    message = lookup.message,
                    confirmLabel = stringResource(R.string.mobile_lookup_retry),
                    onConfirm = {
                        showLookupResultDialog = false
                        viewModel.fetchRecipientName()
                    },
                    dismissLabel = stringResource(R.string.mobile_lookup_cancel),
                    onDismiss = { showLookupResultDialog = false }
                )
                MobileLookupState.Idle, MobileLookupState.Looking -> Unit
            }
        }

        if (gateOpen && mode != OperationMode.MANUAL) {
            PinGate(
                title = "Authorize payment",
                statement = "₹${ui.amount.ifBlank { "0" }}",
                hint = buildString {
                    append("→ ")
                    append(ui.payeeName.ifBlank { ui.vpa }.ifBlank { "RECIPIENT" })
                    append("  ·  ")
                    append("$pinLength DIGITS")
                },
                pin = ui.pin,
                pinLength = pinLength,
                onPinChange = viewModel::onPinChanged,
                onDismiss = {
                    gateOpen = false
                    viewModel.onPinChanged("")
                },
                useBoxes = true,
                error = ui.errors[FormField.PIN]
            )
        }

        SnackbarStrip(
            message = snackbar,
            onDismiss = viewModel::dismissSnackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

// ─── Form ──────────────────────────────────────────────────────────────────────

@Composable
private fun PayForm(
    ui: PayUiState,
    mode: OperationMode,
    permissions: PermissionStatus,
    onPay: () -> Unit,
    onScan: () -> Unit,
    onHistory: () -> Unit,
    onHelp: () -> Unit,
    onPayModeChanged: (PayMode) -> Unit,
    onMobileChanged: (String) -> Unit,
    onFetchName: () -> Unit,
    onVpa: (String) -> Unit,
    onAmount: (String) -> Unit,
    onNote: (String) -> Unit,
    onOpenGate: () -> Unit,
    pinLength: Int = 6,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val launchers = rememberPermissionLaunchers()
    val scrollState = rememberScrollState()

    // While a line field holds focus the IME owns entry — step the pad
    // aside so the two never fight for the bottom of the screen.
    var lineFieldFocused by remember { mutableStateOf(false) }

    val vpaLooksValid = ui.vpa.contains('@') && ui.vpa.substringAfter('@').isNotEmpty()
    val amountLooksValid = ui.amount.toDoubleOrNull()?.let { it > 0.0 } == true

    // MANUAL mode falls straight through to the ViewModel (clipboard +
    // dialer). Otherwise: validate first; if clean, open the PIN gate.
    val onBarClick: () -> Unit = handler@{
        if (mode == OperationMode.MANUAL) {
            onPay()
            return@handler
        }
        if (!vpaLooksValid || !amountLooksValid) {
            onPay() // surfaces field-level errors through the ViewModel
            return@handler
        }
        onOpenGate()
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Title block ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Pay",
                    style = OffipeType.DisplayMedium,
                    color = OffipeColors.TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Tag(
                    "Offline UPI · *99#",
                    led = if (permissions.phoneBundle) OffipeColors.Success
                    else OffipeColors.Warn,
                    ledPulse = permissions.phoneBundle,
                    color = OffipeColors.TextSecondary
                )
            }
            IconKey(
                icon = Icons.Filled.History,
                contentDescription = "History",
                onClick = onHistory,
                size = 42.dp,
                tint = OffipeColors.TextSecondary
            )
            Spacer(Modifier.width(8.dp))
            IconKey(
                icon = Icons.Outlined.HelpOutline,
                contentDescription = "Help",
                onClick = onHelp,
                size = 42.dp,
                tint = OffipeColors.TextSecondary
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            PermissionGate(
                permissions = permissions,
                mode = mode,
                launchers = launchers,
                context = context
            )

            if (permissions.phoneBundle &&
                (mode == OperationMode.MANUAL || permissions.accessibility) &&
                (mode == OperationMode.MANUAL || permissions.overlay)
            ) {
                Spacer(Modifier.height(4.dp))
            }

            Segmented(
                options = listOf(
                    stringResource(R.string.pay_mode_upi_id),
                    stringResource(R.string.pay_mode_mobile_number)
                ),
                selectedIndex = if (ui.payMode == PayMode.UPI_ID) 0 else 1,
                onSelect = { index ->
                    focusManager.clearFocus()
                    lineFieldFocused = false
                    onPayModeChanged(if (index == 0) PayMode.UPI_ID else PayMode.MOBILE)
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (ui.payMode == PayMode.UPI_ID) {
                // ── Destination ──
                InlineField(
                    value = ui.vpa,
                    onValueChange = onVpa,
                    label = "Recipient UPI ID",
                    placeholder = "name@bank",
                    error = ui.errors[FormField.VPA],
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    onFocusChange = { lineFieldFocused = it },
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        IconKey(
                            icon = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR code",
                            onClick = onScan,
                            size = 38.dp,
                            tint = OffipeColors.TextSecondary
                        )
                    }
                )

                Spacer(Modifier.height(18.dp))

                // ── Amount readout ──
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        style = OffipeType.AmountHero.copy(
                            color = OffipeColors.TextSecondary
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = ui.amount.ifBlank { "0" },
                        style = OffipeType.AmountHero.copy(
                            color = if (ui.amount.isBlank()) OffipeColors.TextMuted
                            else OffipeColors.TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (ui.amount.isNotEmpty()) {
                        IconKey(
                            icon = Icons.Filled.Close,
                            contentDescription = "Clear amount",
                            onClick = { onAmount("") },
                            size = 36.dp,
                            tint = OffipeColors.TextMuted
                        )
                    }
                }
                if (ui.errors[FormField.AMOUNT] != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = ui.errors[FormField.AMOUNT] ?: "",
                        style = OffipeType.BodySmall,
                        color = OffipeColors.Danger
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    Tag("Range ₹1 — ₹5,000")
                }

                Spacer(Modifier.height(18.dp))

                // ── Note ──
                InlineField(
                    value = ui.note,
                    onValueChange = onNote,
                    label = "Note (optional)",
                    placeholder = "For coffee",
                    error = ui.errors[FormField.NOTE],
                    onFocusChange = { lineFieldFocused = it },
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        if (ui.note.isNotEmpty()) {
                            IconKey(
                                icon = Icons.Filled.Close,
                                contentDescription = "Clear note",
                                onClick = { onNote("") },
                                size = 38.dp,
                                tint = OffipeColors.TextMuted
                            )
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))
            } else {
                InlineField(
                    value = ui.mobile,
                    onValueChange = onMobileChanged,
                    label = stringResource(R.string.mobile_number_label),
                    placeholder = stringResource(R.string.mobile_number_placeholder),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    onFocusChange = { lineFieldFocused = it },
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                if (mode == OperationMode.MANUAL || !permissions.accessibility) {
                    AlertStrip(
                        title = stringResource(R.string.mobile_lookup_needs_auto_title),
                        message = stringResource(R.string.mobile_lookup_needs_auto),
                        tone = AlertTone.Info,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (MobileLookupParser.isValidMobile(ui.mobile)) {
                    PrimaryActionBar(
                        text = stringResource(R.string.mobile_fetch_name),
                        onClick = onFetchName,
                        enabled = mode != OperationMode.MANUAL &&
                            permissions.phoneBundle && permissions.accessibility &&
                            ui.lookupState !is MobileLookupState.Looking,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (ui.lookupState is MobileLookupState.Linked) {
                    Spacer(Modifier.height(12.dp))
                    PanelCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Tag(stringResource(R.string.mobile_lookup_recipient))
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = MobileLookupParser.titleCaseName(ui.lookupState.name),
                                style = OffipeType.TitleMedium,
                                color = OffipeColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // ── Numeric pad — the amount machine ──
        if (ui.payMode == PayMode.UPI_ID && !lineFieldFocused) {
            NumericPad(
                onDigit = { d ->
                    val candidate = ui.amount + d
                    if (candidate.matches(AMOUNT_ENTRY_PATTERN) &&
                        (d != '.' || !ui.amount.contains('.'))
                    ) {
                        onAmount(candidate)
                    }
                },
                onDelete = { if (ui.amount.isNotEmpty()) onAmount(ui.amount.dropLast(1)) },
                onClear = { if (ui.amount.isNotEmpty()) onAmount("") },
                showDecimal = true
            )
        }

        // ── Docked pill ──
        Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            PrimaryActionBar(
                text = if (ui.payMode == PayMode.MOBILE) {
                    stringResource(R.string.mobile_pay_coming_soon)
                } else when {
                    mode == OperationMode.MANUAL -> "Open Dialer"
                    ui.amount.isNotBlank() -> "Pay ₹${ui.amount}"
                    else -> "Next"
                },
                onClick = if (ui.payMode == PayMode.MOBILE) ({}) else onBarClick,
                enabled = ui.payMode == PayMode.UPI_ID && permissions.readyForDialerPay,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── Permission notices ────────────────────────────────────────────────────────

@Composable
private fun PermissionGate(
    permissions: PermissionStatus,
    mode: OperationMode,
    launchers: com.offipe.app.presentation.permissions.PermissionLaunchers,
    context: android.content.Context
) {
    if (!permissions.phoneBundle) {
        AlertStrip(
            title = "Phone permission required",
            message = "Offipe needs phone access to dial *99# for offline payments.",
            tone = AlertTone.Danger,
            actionLabel = "Grant",
            onAction = { launchers.requestPhoneBundle() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
    }
    // Manual mode doesn't use accessibility or the overlay — don't nag.
    if (mode != OperationMode.MANUAL) {
        if (!permissions.accessibility) {
            AlertStrip(
                title = "Accessibility not enabled",
                message = "Turn on Offipe's accessibility service to automate the carrier dialog.",
                tone = AlertTone.Warn,
                actionLabel = "Grant",
                onAction = { openAccessibilitySettings(context) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }
        if (!permissions.overlay) {
            AlertStrip(
                title = "Overlay permission off",
                message = "Allow Offipe to display over other apps to cover the system USSD dialog.",
                tone = AlertTone.Warn,
                actionLabel = "Grant",
                onAction = { openOverlaySettings(context) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
