package com.offipe.app.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Offipe typography — one strong sans family, hierarchy through weight
 * and size.
 *
 *  - Display sizes are heavy and tightly tracked: screen titles and
 *    hero statements ("Pay Offline.", "Payment Successful").
 *  - Amounts are the same sans, bold with tabular figures (`tnum`) so
 *    digits never jitter as they change.
 *  - Labels are small, medium-weight, slightly tracked uppercase — the
 *    quiet wayfinding layer (field labels, section overlines, tags).
 */
object OffipeType {
    val Display = FontFamily.SansSerif
    val Body = FontFamily.SansSerif
    val MonoFamily = FontFamily.SansSerif

    val DisplayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = (-0.03).em,
        lineHeight = 44.sp
    )
    val DisplayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 36.sp
    )
    val DisplaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.015).em,
        lineHeight = 30.sp
    )
    val HeadlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        letterSpacing = (-0.01).em,
        lineHeight = 25.sp
    )
    val TitleLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.em,
        lineHeight = 22.sp
    )
    val TitleMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.005).em,
        lineHeight = 20.sp
    )
    val BodyLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.002.em,
        lineHeight = 23.sp
    )
    val BodyMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.002.em,
        lineHeight = 21.sp
    )
    val BodySmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        letterSpacing = 0.005.em,
        lineHeight = 18.sp
    )
    val LabelLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.01.em
    )
    val LabelMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.01.em
    )
    val LabelSmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.06.em
    )
    val Mono = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        fontFeatureSettings = "tnum"
    )
    /** Small uppercase overline — field labels, section headers, tags. */
    val TerminalLabel = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.10.em
    )

    /** Pay screen — the amount readout. Bold, huge, tabular. */
    val AmountHero = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        letterSpacing = (-0.04).em,
        lineHeight = 56.sp,
        fontFeatureSettings = "tnum"
    )

    /** Balance screen — the balance figure, one step below hero. */
    val AmountBalance = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = (-0.035).em,
        lineHeight = 46.sp,
        fontFeatureSettings = "tnum"
    )

    /** Inline money at large size (session results, expanded rows). */
    val AmountLg = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 30.sp,
        fontFeatureSettings = "tnum"
    )

    /** Inline money at medium size (ledger rows). */
    val AmountMd = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum"
    )

    /** Keypad key digits — bold, centered. */
    val KeyDigit = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        letterSpacing = 0.em
    )
}

val OffipeTypography = Typography(
    displayLarge = OffipeType.DisplayLarge,
    displayMedium = OffipeType.DisplayMedium,
    displaySmall = OffipeType.DisplaySmall,
    headlineLarge = OffipeType.HeadlineLarge,
    titleLarge = OffipeType.TitleLarge,
    titleMedium = OffipeType.TitleMedium,
    bodyLarge = OffipeType.BodyLarge,
    bodyMedium = OffipeType.BodyMedium,
    bodySmall = OffipeType.BodySmall,
    labelLarge = OffipeType.LabelLarge,
    labelMedium = OffipeType.LabelMedium,
    labelSmall = OffipeType.LabelSmall
)
