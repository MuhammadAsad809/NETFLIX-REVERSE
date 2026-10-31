package com.netflix.moviebox.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixDimens

@OptIn(UnstableApi::class)
@Composable
fun HeroBillboard(
    movie: MovieBoxItem?,
    onPlayClick: (MovieBoxItem) -> Unit,
    onDetailsClick: (MovieBoxItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (movie == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(NetflixDimens.HeroHeight)
                .shimmer()
        )
        return
    }

    val context = LocalContext.current
    var isInList by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    val trailerUrl = movie.trailerUrl

    DisposableEffect(trailerUrl) {
        if (!trailerUrl.isNullOrEmpty()) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(trailerUrl))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            isVideoPlaying = true
                        }
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        isVideoPlaying = false
                    }
                })
                prepare()
            }
            exoPlayer = player
            onDispose {
                player.release()
                exoPlayer = null
                isVideoPlaying = false
            }
        } else {
            onDispose { }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NetflixDimens.HeroHeight)
    ) {
        // 1. Poster Backdrop (always present as base / loading placeholder)
        AsyncImage(
            model = movie.stillsUrl ?: movie.coverUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Auto-playing Trailer (fades in smoothly once ready)
        if (!trailerUrl.isNullOrEmpty() && exoPlayer != null) {
            AnimatedVisibility(
                visible = isVideoPlaying,
                enter = fadeIn(animationSpec = tween(600)),
                exit = fadeOut(animationSpec = tween(400)),
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    update = { view ->
                        view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 3. Multi-stop Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            NetflixColors.BackgroundDark
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 4. Mute / Unmute Floating Action Button (Netflix style)
        if (!trailerUrl.isNullOrEmpty() && isVideoPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 16.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    .clickable {
                        isMuted = !isMuted
                        exoPlayer?.volume = if (isMuted) 0f else 1f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 3. Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "N",
                color = NetflixColors.NetflixRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NetflixColors.PureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 4. Hero Information & Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language & Score Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = NetflixColors.NetflixRed,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = movie.detectedLang.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${movie.score} Match",
                    color = Color(0xFF46D369),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = movie.releaseDate,
                    color = NetflixColors.TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = movie.title,
                color = NetflixColors.PureWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: My List | Play | Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // My List with Lottie morph
                LottieMyListButton(
                    isInList = isInList,
                    onToggle = { isInList = it }
                )

                // Play Button
                Button(
                    onClick = { onPlayClick(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixColors.PureWhite),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .width(115.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Info Action
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onDetailsClick(movie) }
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = NetflixColors.PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Info",
                        color = NetflixColors.PureWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
