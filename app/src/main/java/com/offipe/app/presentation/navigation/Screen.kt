package com.offipe.app.presentation.navigation

/**
 * Navigation routes for the single-activity architecture.
 *
 * HUB MODEL: [Home] is the start destination — every feature hangs off
 * it as a push destination (Pay / Scan / QR result / Balance / History /
 * Settings), and reader screens (FAQ / legal) sit on top of those.
 *
 * Splash and onboarding are handled outside the NavHost (first-launch
 * gate in OffipeApp), so they have no routes.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Pay : Screen("pay")
    object Scan : Screen("scan")
    object QrResult : Screen("qrresult")
    object Balance : Screen("balance")
    object Settings : Screen("settings")
    object History : Screen("history")
    object Faq : Screen("faq")
    object Legal : Screen("legal")
    object Privacy : Screen("privacy")
    object Terms : Screen("terms")
}
