package com.offipe.app.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.offipe.app.R
import com.offipe.app.presentation.ui.components.WaveMotif
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeType
import com.offipe.app.presentation.ui.components.offipeLogoPainter
import kotlinx.coroutines.delay

/**
 * SPLASH — the app opens on the mark over a quiet wave field, with the
 * OFFLINE UPI · *99# signature and a thin progress rule that fills while
 * the first frame settles, then hands off to onboarding or the hub.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "splash_progress"
    )

    LaunchedEffect(Unit) {
        progress = 1f
        delay(1200)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(OffipeColors.Black)
    ) {
        WaveMotif(Modifier.fillMaxSize(), strength = 0.8f)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = offipeLogoPainter(),
                contentDescription = "Offipe",
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "OFFLINE UPI",
                style = OffipeType.TerminalLabel.copy(textAlign = TextAlign.Center),
                color = OffipeColors.TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "*99#",
                style = OffipeType.TerminalLabel.copy(textAlign = TextAlign.Center),
                color = OffipeColors.TextMuted
            )
        }

        // Progress rule near the bottom edge. The fill is DRAWN from the
        // animated value instead of being read as a width during
        // composition, so the splash no longer recomposes every frame.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 40.dp, end = 40.dp, bottom = 56.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(OffipeColors.Border)
                .drawBehind {
                    drawRect(
                        color = OffipeColors.Mark,
                        size = Size(size.width * animated, size.height)
                    )
                }
        )
    }
}
