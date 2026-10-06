package com.offipe.app.presentation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Offipe shapes — soft but restrained.
 *
 * Cards and panels sit at a quiet 16dp; fields at 12dp; small keys at
 * 10dp; buttons are full pills (the reference's primary CTAs), and the
 * bottom sheet keeps the generous sweep so it separates from its scrim.
 */
val OffipeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
