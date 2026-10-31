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
        iterations = 1,
        restartOnPlay = false
    )

    var finished by remember { mutableStateOf(false) }

    // Trigger iconic studio Ta-Dum sound on launch
    LaunchedEffect(Unit) {
        delay(150)
        soundManager.playChime()
    }

    // When 2.0s animation finishes smoothly, transition to Home
    LaunchedEffect(progress) {
        if (progress >= 0.98f && !finished) {
            finished = true
            delay(350)
            onSplashFinished()
        }
    }

    // Safety fallback timeout to guarantee transition even on slow devices
    LaunchedEffect(Unit) {
        delay(2600)
        if (!finished) {
            finished = true
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
            modifier = Modifier.size(200.dp)
        )
    }
}
