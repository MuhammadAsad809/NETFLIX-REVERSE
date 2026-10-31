package com.netflix.moviebox.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixDimens
import kotlinx.coroutines.delay

@Composable
fun HeroBillboard(
    movie: MovieBoxItem?,
    onPlayClick: (MovieBoxItem) -> Unit,
    onDetailsClick: (MovieBoxItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val list = if (movie != null) listOf(movie) else emptyList()
    HeroBillboard(
        heroes = list,
        onPlayClick = onPlayClick,
        onDetailsClick = onDetailsClick,
        onSearchClick = onSearchClick,
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Composable
fun HeroBillboard(
    heroes: List<MovieBoxItem>,
    onPlayClick: (MovieBoxItem) -> Unit,
    onDetailsClick: (MovieBoxItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (heroes.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(NetflixDimens.HeroHeight)
                .shimmer()
        )
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, heroes.size - 1)
    val currentMovie = heroes[safeIndex]

    // Auto-rotate hero every 14 seconds
    LaunchedEffect(heroes.size, currentIndex) {
        if (heroes.size > 1) {
            delay(14000L)
            currentIndex = (currentIndex + 1) % heroes.size
        }
    }

    val context = LocalContext.current
    var isInList by remember { mutableStateOf(false) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    val trailerUrl = currentMovie.trailerUrl

    DisposableEffect(currentMovie.subjectId, trailerUrl) {
        isVideoPlaying = false
        if (!trailerUrl.isNullOrEmpty()) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000)

            val mediaSourceFactory = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory)

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(trailerUrl))
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = if (isMuted) 0f else 1f
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
        // 1. Poster Backdrop (Always smooth crossfade on movie switch)
        Crossfade(
            targetState = currentMovie,
            animationSpec = tween(700),
            modifier = Modifier.fillMaxSize()
        ) { movie ->
            AsyncImage(
                model = movie.stillsUrl ?: movie.coverUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

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

        // 4. Next / Prev Chevron Controls (Allows manual hero switching)
        if (heroes.size > 1) {
            // Left Chevron
            IconButton(
                onClick = {
                    currentIndex = if (currentIndex > 0) currentIndex - 1 else heroes.size - 1
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Hero",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }

            // Right Chevron
            IconButton(
                onClick = {
                    currentIndex = (currentIndex + 1) % heroes.size
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Hero",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // 5. Mute / Unmute Floating Action Button
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

        // 6. Top Navigation Header
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

        // 7. Hero Information & Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
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
                        text = currentMovie.detectedLang.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${currentMovie.score} Match",
                    color = Color(0xFF46D369),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentMovie.releaseDate,
                    color = NetflixColors.TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = currentMovie.title,
                color = NetflixColors.PureWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

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

                // Play Button (Launches Full Movie)
                Button(
                    onClick = { onPlayClick(currentMovie) },
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
                    modifier = Modifier.clickable { onDetailsClick(currentMovie) }
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

            Spacer(modifier = Modifier.height(10.dp))

            // 8. Carousel Progress Indicator Dots
            if (heroes.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    heroes.take(7).forEachIndexed { idx, _ ->
                        val isSelected = idx == safeIndex
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 7.dp else 5.dp)
                                .background(
                                    color = if (isSelected) NetflixColors.NetflixRed else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { currentIndex = idx }
                        )
                    }
                }
            }
        }
    }
}
