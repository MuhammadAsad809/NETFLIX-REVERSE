package com.netflix.moviebox.ui.screens

import androidx.compose.foundation.background
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

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottiefiles/glowing-netflix-n-logo.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    // Trigger studio chime sound on launch
    LaunchedEffect(Unit) {
        delay(200)
        soundManager.playChime()
    }

    // When 2.0s animation finishes, transition to Home
    LaunchedEffect(progress) {
        if (progress == 1f) {
            delay(300)
            onSplashFinished()
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(160.dp)
        )
    }
}
