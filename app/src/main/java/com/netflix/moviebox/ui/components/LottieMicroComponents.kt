package com.netflix.moviebox.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.netflix.moviebox.ui.theme.NetflixMotion

@Composable
fun LottieMyListButton(
    isInList: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottiefiles/my-list-plus-to-check.json")
    )

    val progress by animateFloatAsState(
        targetValue = if (isInList) 1f else 0f,
        animationSpec = tween(
            durationMillis = NetflixMotion.MY_LIST_DURATION_MS,
            easing = NetflixMotion.DecelerateEasing
        ),
        label = "plus_to_check"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle(!isInList)
            }
            .padding(8.dp)
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isInList) "Added" else "My List",
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
fun LottieEqualizer(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottiefiles/now_playing.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(28.dp)
    )
}

@Composable
fun LottieLiveBadge(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottiefiles/live_now.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(20.dp)
    )
}
