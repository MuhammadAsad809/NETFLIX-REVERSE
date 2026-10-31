package com.netflix.moviebox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixMotion

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = NetflixMotion.SHIMMER_SWEEP_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val colors = listOf(
        NetflixColors.ShimmerBase,
        NetflixColors.ShimmerHighlight,
        NetflixColors.ShimmerBase
    )

    background(
        brush = Brush.linearGradient(
            colors = colors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )
    )
}

@Composable
fun ShimmerPoster(
    modifier: Modifier = Modifier,
    width: Dp = 115.dp,
    height: Dp = 175.dp
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shimmer()
            .background(NetflixColors.CardSurface, RoundedCornerShape(4.dp))
    )
}

@Composable
fun ShimmerPosterItem(
    modifier: Modifier = Modifier,
    width: Dp = 115.dp,
    height: Dp = 175.dp
) {
    ShimmerPoster(modifier, width, height)
}

@Composable
fun ShimmerHeroItem(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(490.dp)
            .shimmer()
    )
}

