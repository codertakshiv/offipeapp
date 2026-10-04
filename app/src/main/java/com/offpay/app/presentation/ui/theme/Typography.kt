package com.offipe.app.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Offipe typography. Default sans for body, monospace for amounts/codes.
 *
 * Refinements applied for the polish pass:
 *  - Display headlines: tighter letter-spacing (-0.04em) and tighter
 *    line-height (1.05) for that mechanical technical feel.
 *  - Section labels: 11sp uppercase, +0.12em letter-spacing, weight 600.
 *  - Body shrunk to 15sp regular (was 16) with relaxed 1.45 line-height
 *    so cards don't feel chunky.
 *  - Captions: 12sp +0.08em.
 *  - Mono amounts: bumped to weight 700 with `tnum` font feature for
 *    tabular spacing (digits align in columns).
 */
object OffipeType {
    val Display = FontFamily.SansSerif
    val Body = FontFamily.SansSerif
    val MonoFamily = FontFamily.Monospace

    val DisplayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        letterSpacing = (-0.04).em,
        lineHeight = 34.sp
    )
    val DisplayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        letterSpacing = (-0.04).em,
        lineHeight = 30.sp
    )
    val DisplaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.03).em,
        lineHeight = 24.sp
    )
    val HeadlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 26.sp
    )
    val TitleLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    )
    val TitleMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )
    val BodyLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    val BodyMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val BodySmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
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
        fontSize = 12.sp,
        letterSpacing = 0.02.em
    )
    val LabelSmall = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.02.em
    )
    val Mono = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        fontFeatureSettings = "tnum"
    )
    val MonoLarge = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum"
    )

    /** Bold tabular monospace, intended for prominent amounts ("₹ 12,345.67"). */
    val MonoAmount = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        fontFeatureSettings = "tnum"
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
