package com.offipe.app.presentation.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.offipe.app.OffipeApplication
import com.offipe.app.domain.SessionState
import com.offipe.app.offipeApp
import com.offipe.app.presentation.BalanceViewModel
import com.offipe.app.presentation.HistoryViewModel
import com.offipe.app.presentation.PayViewModel
import com.offipe.app.presentation.permissions.rememberPermissionStatus
import com.offipe.app.presentation.screens.BalanceScreen
import com.offipe.app.presentation.screens.FaqScreen
import com.offipe.app.presentation.screens.HistoryScreen
import com.offipe.app.presentation.screens.HomeScreen
import com.offipe.app.presentation.screens.LegalScreen
import com.offipe.app.presentation.screens.PayScreen
import com.offipe.app.presentation.screens.QrResultScreen
import com.offipe.app.presentation.screens.ScanScreen
import com.offipe.app.presentation.screens.SettingsScreen
import com.offipe.app.presentation.screens.SplashScreen
import com.offipe.app.presentation.screens.onboarding.OnboardingFlow
import com.offipe.app.presentation.ui.components.MoneyRainOverlay
import com.offipe.app.presentation.ui.components.SessionOverlay
import com.offipe.app.presentation.ui.theme.OffipeColors
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Top-level scaffold:
 *  - [SplashScreen] holds cold start until the first-launch flag has
 *    actually resolved, so the very first frame of real UI is either
 *    onboarding or Home — never the wrong one (no Home flash on a fresh
 *    install, no onboarding flash for a returning user).
 *  - NAVIGATION = hub model: [HomeScreen] is the start destination and
 *    every feature pushes on top of it; reader routes render full-bleed.
 *  - USSD sessions are taken over by [SessionOverlay] rendered HERE, at
 *    scaffold level, from either ViewModel's session state — screens stay
 *    pure forms underneath.
 *  - PIN entry is inline via the PIN gate on Pay & Balance forms; there
 *    is no standalone PIN screen.
 *  - Money-rain easter egg: 5 taps on the Home mark.
 *  - The scanned QR payload is held here so [QrResultScreen] and the Pay
 *    form share it without serializing through route args.
 */
@Composable
fun OffipeApp() {
    val app = LocalContext.current.offipeApp

    // The FIRST-LAUNCH flag is read with a `null` sentinel, never an
    // optimistic default. DataStore is asynchronous: with `initial = true`
    // the flow briefly reported "setup done" before its first real value
    // arrived, so a fresh install painted Home for a few frames and then
    // swapped to onboarding. `null` means "not loaded yet" and keeps us on
    // the splash branch until the destination is actually known.
    val firstLaunchDone by app.prefsRepo.firstLaunchComplete
        .map<Boolean, Boolean?> { it }
        .collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var splashDone by remember { mutableStateOf(false) }

    when {
        // Stay on splash until BOTH the splash timing finished and the
        // first-launch flag resolved — the main UI is never rendered
        // before the initial route is decided.
        !splashDone || firstLaunchDone == null ->
            SplashScreen(onDone = { splashDone = true })

        firstLaunchDone == false -> OnboardingFlow(
            onComplete = {
                scope.launch { app.prefsRepo.setFirstLaunchComplete(true) }
            },
            prefsRepo = app.prefsRepo
        )

        else -> MainScaffold(app = app)
    }
}

@Composable
private fun MainScaffold(app: OffipeApplication) {
    val navController = rememberNavController()
    val backstackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backstackEntry?.destination?.route ?: Screen.Home.route
    val view = LocalView.current

    val permissionsState = rememberPermissionStatus()
    val permissions by permissionsState
    val userName by app.prefsRepo.userName.collectAsState(initial = "")

    val payViewModel = payViewModel(app)
    val balanceViewModel = balanceViewModel(app)
    val historyViewModel = rememberHistoryViewModel(app)

    val paySession by payViewModel.sessionState.collectAsState()
    val balanceSession by balanceViewModel.sessionState.collectAsState()

    val payActive = paySession !is SessionState.Idle
    val balanceActive = balanceSession !is SessionState.Idle
    val session: SessionState = when {
        payActive -> paySession
        balanceActive -> balanceSession
        else -> SessionState.Idle
    }

    // Money-rain easter egg — 5 taps on the Home mark within 1.5s
    var markTaps by remember { mutableIntStateOf(0) }
    var markWindowStart by remember { mutableLongStateOf(0L) }
    var moneyRain by remember { mutableStateOf(false) }

    fun onMarkTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - markWindowStart > 1500L) {
            markWindowStart = now
            markTaps = 1
        } else {
            markTaps += 1
        }
        if (markTaps >= 5) {
            markTaps = 0
            markWindowStart = 0L
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            moneyRain = true
        }
    }

    // Raw QR payload from the scanner → QR result → Pay form.
    var pendingQr by remember { mutableStateOf<String?>(null) }

    fun goTo(route: String) {
        if (route != currentRoute) {
            navController.navigate(route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun push(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    historyViewModel = historyViewModel,
                    permissions = permissions,
                    onNavigatePay = { goTo(Screen.Pay.route) },
                    onNavigateScan = { push(Screen.Scan.route) },
                    onNavigateBalance = { goTo(Screen.Balance.route) },
                    onNavigateHistory = { push(Screen.History.route) },
                    onNavigateSettings = { goTo(Screen.Settings.route) },
                    onMarkTap = ::onMarkTap,
                    userName = userName
                )
            }
            composable(Screen.Pay.route) {
                PayScreen(
                    viewModel = payViewModel,
                    onNavigateScan = { push(Screen.Scan.route) },
                    onNavigateHistory = { push(Screen.History.route) },
                    onNavigateFaq = { push(Screen.Faq.route) },
                    permissions = permissionsState
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    qrManager = app.qrScannerManager,
                    onResult = { raw ->
                        pendingQr = raw
                        push(Screen.QrResult.route)
                    },
                    onClose = { navController.popBackStack() },
                    onOpenFaq = { push(Screen.Faq.route) }
                )
            }
            composable(Screen.QrResult.route) {
                QrResultScreen(
                    raw = pendingQr,
                    payViewModel = payViewModel,
                    onContinueToPay = {
                        pendingQr = null
                        goTo(Screen.Pay.route)
                    },
                    onBack = {
                        pendingQr = null
                        navController.popBackStack()
                    },
                    onScanAgain = {
                        pendingQr = null
                        navController.popBackStack(Screen.Scan.route, inclusive = false)
                    }
                )
            }
            composable(Screen.Balance.route) {
                BalanceScreen(
                    viewModel = balanceViewModel,
                    historyViewModel = historyViewModel,
                    onOpenHistory = { push(Screen.History.route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onPay = { goTo(Screen.Pay.route) },
                    onPayAgain = { txn ->
                        // Pre-fill the Pay form with this transaction's
                        // recipient + amount + note, then route back.
                        // The user still has to enter a PIN before any
                        // money moves — we never re-execute silently.
                        payViewModel.prefillFromTransaction(
                            vpa = txn.vpa,
                            amount = txn.amount,
                            note = txn.note
                        )
                        goTo(Screen.Pay.route)
                    },
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    prefsRepo = app.prefsRepo,
                    historyViewModel = historyViewModel,
                    permissions = permissions,
                    versionName = "1.0.1",
                    onClearAllData = {
                        historyViewModel.clearAllData {
                            app.prefsRepo.clearLastBalance()
                            app.cacheDir.resolve("shared/Offipe.apk").delete()
                        }
                    },
                    onOpenFaq = { push(Screen.Faq.route) },
                    onOpenPrivacy = { push(Screen.Privacy.route) },
                    onOpenTerms = { push(Screen.Terms.route) }
                )
            }
            composable(Screen.Faq.route) {
                FaqScreen(
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Screen.Legal.route) {
                LegalScreen(onClose = { navController.popBackStack() }, initialTab = 0)
            }
            // Privacy and Terms open the same document on its own tab
            composable(Screen.Privacy.route) {
                LegalScreen(onClose = { navController.popBackStack() }, initialTab = 0)
            }
            composable(Screen.Terms.route) {
                LegalScreen(onClose = { navController.popBackStack() }, initialTab = 1)
            }
        }

        if (currentRoute == Screen.Pay.route) {
            BackHandler {
                when (payViewModel.sessionState.value) {
                    is SessionState.Running -> payViewModel.cancelSession()
                    is SessionState.Success, is SessionState.Failed -> payViewModel.dismissSession()
                    SessionState.Idle -> Unit
                }
                payViewModel.onNavigateAway()
                navController.popBackStack()
            }
        }

        // ── Session takeover: one overlay for both flows, above everything ──
        if (session !is SessionState.Idle) {
            SessionOverlay(
                state = session,
                onCancel = {
                    if (payActive) payViewModel.cancelSession()
                    else balanceViewModel.cancelSession()
                },
                onDone = {
                    if (payActive) payViewModel.dismissSession()
                    else balanceViewModel.dismissSession()
                },
                onRetry = {
                    if (payActive) payViewModel.dismissSession()
                    else balanceViewModel.dismissSession()
                },
                successLabel = if (payActive) "PAYMENT AUTHORIZED" else "BALANCE CHECKED",
                onWhyFailed = if (payActive) {
                    {
                        payViewModel.dismissSession()
                        push(Screen.Faq.route)
                    }
                } else null
            )
        }

        if (moneyRain) {
            MoneyRainOverlay(onDone = { moneyRain = false })
        }
    }
}

/**
 * Writes [text] to the system clipboard with a "UPI ID" label. Used by
 * MANUAL mode to pre-stage the recipient VPA before opening the dialer.
 */
private fun writeToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", text))
}

/**
 * Shows a system-level Toast that survives our activity losing focus —
 * critical for MANUAL mode, where we hand off to the native dialer the
 * moment after copying the UPI ID. The Compose snackbar inside the app
 * disappears the instant the dialer takes the foreground.
 *
 * Uses applicationContext so the Toast queue isn't tied to the activity
 * that triggered it (it's still the user's request, this just keeps the
 * Toast alive across the activity transition).
 */
private fun showSystemToast(context: Context, text: String) {
    Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG).show()
}

@Composable
private fun payViewModel(app: OffipeApplication): PayViewModel {
    val context = LocalContext.current.applicationContext
    val factory = remember(app, context) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(PayViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return PayViewModel(
                    actionRunner = app.actionRunner,
                    historyRepo = app.historyRepo,
                    prefsRepo = app.prefsRepo,
                    carrierDetector = app.carrierDetector,
                    overlayController = app.overlayController,
                    onDialerFallback = { code ->
                        val encodedCode = code.replace("#", Uri.encode("#"))
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedCode"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    clipboardWriter = { text -> writeToClipboard(context, text) },
                    systemToast = { text -> showSystemToast(context, text) },
                    stringFor = context::getString
                ) as T
            }
        }
    }
    return viewModel(factory = factory)
}

@Composable
private fun balanceViewModel(app: OffipeApplication): BalanceViewModel {
    val context = LocalContext.current.applicationContext
    val factory = remember(app, context) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(BalanceViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return BalanceViewModel(
                    actionRunner = app.actionRunner,
                    prefsRepo = app.prefsRepo,
                    overlayController = app.overlayController,
                    onDialerFallback = { code ->
                        val encodedCode = code.replace("#", Uri.encode("#"))
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedCode"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    clipboardWriter = { text -> writeToClipboard(context, text) },
                    systemToast = { text -> showSystemToast(context, text) }
                ) as T
            }
        }
    }
    return viewModel(factory = factory)
}

@Composable
private fun rememberHistoryViewModel(app: OffipeApplication): HistoryViewModel {
    return remember { HistoryViewModel(app.historyRepo) }
}
