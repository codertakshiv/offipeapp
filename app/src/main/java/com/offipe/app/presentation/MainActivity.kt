package com.offipe.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.offipe.app.presentation.navigation.OffipeApp
import com.offipe.app.presentation.ui.theme.OffipeColors
import com.offipe.app.presentation.ui.theme.OffipeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Activity launched under Theme.Offipe.Splash; swap to the real
        // theme so edge-to-edge colours and statusBar transparency apply.
        // On API 31+ the system splash composes from windowSplashScreen*
        // attributes in values-v31/themes.xml and dismisses itself once
        // the first window is drawn — no extra library needed.
        setTheme(com.offipe.app.R.style.Theme_Offipe)
        enableEdgeToEdge()
        setContent {
            OffipeTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(OffipeColors.Black)
                ) {
                    OffipeApp()
                }
            }
        }
    }
}