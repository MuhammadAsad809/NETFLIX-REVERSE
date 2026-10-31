package com.netflix.moviebox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.netflix.moviebox.ui.player.SoundManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val currentOnSplashFinished by rememberUpdatedState(onSplashFinished)

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottiefiles/glowing-netflix-n-logo.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        restartOnPlay = true
    )

    // Trigger iconic studio Ta-Dum sound and guarantee transition to Home in a single coroutine
    LaunchedEffect(Unit) {
        delay(100)
        soundManager.playChime()
        // Wait 2.0s for the Netflix N glow animation to complete
        delay(2000)
        currentOnSplashFinished()
    }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { currentOnSplashFinished() }, // Allow tapping screen to skip splash immediately
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(200.dp)
        )
    }
}
