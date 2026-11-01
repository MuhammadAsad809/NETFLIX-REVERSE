package com.netflix.moviebox.ui.player

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.airbnb.lottie.compose.*
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixDimens
import com.netflix.moviebox.ui.theme.NetflixMotion
import kotlinx.coroutines.delay

class PlayerActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: ""
        val movieTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Movie"

        setContent {
            NetflixVideoPlayer(
                streamUrl = streamUrl,
                title = movieTitle,
                onBackPressed = { finish() },
                onPlayerReady = { player -> exoPlayer = player }
            )
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_TITLE = "extra_title"

        fun launch(context: Context, streamUrl: String, title: String) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAM_URL, streamUrl)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun NetflixVideoPlayer(
    streamUrl: String,
    title: String,
    onBackPressed: () -> Unit,
    onPlayerReady: (ExoPlayer) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showUnlockPrompt by remember { mutableStateOf(false) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }

    var selectedQuality by remember { mutableStateOf("1080p") }
    var showQualityMenu by remember { mutableStateOf(false) }

    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    var showRewindRipple by remember { mutableStateOf(false) }
    var showForwardRipple by remember { mutableStateOf(false) }

    // Netflix-Grade Zero-Lag ExoPlayer Engine
    val exoPlayer = remember {
        // 1. High-speed streaming data source with VLC headers & keep-alive
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("VLC/3.0.18 LibVLC/3.0.18")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://themoviebox.xyz/",
                    "Origin" to "https://themoviebox.xyz",
                    "Accept" to "*/*",
                    "Connection" to "keep-alive"
                )
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(25000)

        // 2. Hardware Acceleration with Decoder Fallback
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)
            .setAllowedVideoJoiningTimeMs(5000)

        // 3. Netflix LoadControl: Instant 800ms start buffer & 30s RAM back-buffer
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 90_000,
                /* bufferForPlaybackMs = */ 800,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(
                /* backBufferDurationMs = */ 30_000,
                /* retainBackBufferFromKeyframe = */ true
            )
            .build()

        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val trackSelector = DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = (state == Player.STATE_BUFFERING)
                        totalDuration = duration.coerceAtLeast(0L)
                        bufferedPosition = bufferedPosition.coerceAtLeast(0L)
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
                onPlayerReady(this)
            }
    }

    // Auto-hide controls timer (4s)
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    // Unlock button auto-hide
    LaunchedEffect(showUnlockPrompt) {
        if (showUnlockPrompt) {
            delay(3000)
            showUnlockPrompt = false
        }
    }

    // Ripple dismissal timers
    LaunchedEffect(showRewindRipple) {
        if (showRewindRipple) {
            delay(700)
            showRewindRipple = false
        }
    }
    LaunchedEffect(showForwardRipple) {
        if (showForwardRipple) {
            delay(700)
            showForwardRipple = false
        }
    }

    // Timeline position & buffer sync loop
    LaunchedEffect(isPlaying) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            delay(400)
        }
    }

    // Lottie center spinner composition
    val spinnerComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottiefiles/play-loading-replay.json")
    )
    val spinnerProgress by animateLottieCompositionAsState(
        composition = spinnerComposition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Double-tap Left: Rewind 10s
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                showRewindRipple = true
                            } else if (offset.x > screenWidth * 0.65f) {
                                // Double-tap Right: Fast-Forward 10s
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(totalDuration)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                showForwardRipple = true
                            } else {
                                // Double-tap Center: Toggle Play/Pause
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        }
                    },
                    onTap = {
                        if (isLocked) {
                            showUnlockPrompt = !showUnlockPrompt
                        } else {
                            showControls = !showControls
                        }
                    }
                )
            }
    ) {
        // 1. AndroidView Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Animated Double-Tap Seek Ripples
        if (showRewindRipple) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
                    .size(72.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "-10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "10s",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showForwardRipple) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
                    .size(72.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "+10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "10s",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Screen Locked Floating Unlock Indicator
        if (isLocked && showUnlockPrompt) {
            Surface(
                color = NetflixColors.NetflixRed,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable {
                        isLocked = false
                        showControls = true
                        showUnlockPrompt = false
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Screen Locked • Tap to Unlock",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Netflix Controls Overlay
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NetflixColors.PlayerScrim)
            ) {
                // A. Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Aspect Ratio Button (Fit / Zoom / Stretch)
                    Surface(
                        color = Color(0x66000000),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                        modifier = Modifier.clickable {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    ) {
                        val label = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Fill"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                            else -> "Fit"
                        }
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Speed Selector Menu
                    Box {
                        Surface(
                            color = Color(0x66000000),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                            modifier = Modifier.clickable { showSpeedMenu = true }
                        ) {
                            Text(
                                text = "${selectedSpeed}x",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false },
                            modifier = Modifier.background(Color(0xFF222222))
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (spd == 1.0f) "1.0x (Normal)" else "${spd}x",
                                            color = if (selectedSpeed == spd) NetflixColors.NetflixRed else Color.White,
                                            fontWeight = if (selectedSpeed == spd) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedSpeed = spd
                                        exoPlayer.playbackParameters = PlaybackParameters(spd)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quality Selector Menu
                    Box {
                        Surface(
                            color = Color(0x66000000),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NetflixColors.MovieBoxGreen),
                            modifier = Modifier.clickable { showQualityMenu = true }
                        ) {
                            Text(
                                text = selectedQuality,
                                color = NetflixColors.MovieBoxGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showQualityMenu,
                            onDismissRequest = { showQualityMenu = false },
                            modifier = Modifier.background(Color(0xFF222222))
                        ) {
                            listOf("1080p Full HD", "720p HD", "480p SD", "Auto").forEach { qName ->
                                val qKey = qName.split(" ")[0]
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = qName,
                                            color = if (selectedQuality == qKey) NetflixColors.MovieBoxGreen else Color.White,
                                            fontWeight = if (selectedQuality == qKey) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedQuality = qKey
                                        showQualityMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // B. Left Netflix Screen Lock Button
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(46.dp)
                        .clickable {
                            isLocked = true
                            showControls = false
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Screen",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(12.dp)
                    )
                }

                // C. Center Netflix Playback Controls (-10s | Play/Pause | +10s)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s Button
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                showRewindRipple = true
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier
                                .size(30.dp)
                                .padding(10.dp)
                        )
                    }

                    // Center 64x64dp Play / Pause Circle
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, Color.White),
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isBuffering) {
                                LottieAnimation(
                                    composition = spinnerComposition,
                                    progress = { spinnerProgress },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Forward 10s Button
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(totalDuration)
                                exoPlayer.seekTo(target)
                                currentPosition = target
                                showForwardRipple = true
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier
                                .size(30.dp)
                                .padding(10.dp)
                        )
                    }
                }

                // D. Bottom Netflix Timeline & Scrub Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    // Dual-Track Progress (Buffered progress in background + active playback)
                    val progressRatio = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
                    val bufferedRatio = if (totalDuration > 0) (bufferedPosition.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Background Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF333333))
                        )

                        // Buffered Progress (Netflix Grey Buffer Indicator)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = bufferedRatio)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF666666))
                        )

                        // Interactive Scrubber Slider
                        Slider(
                            value = progressRatio,
                            onValueChange = { ratio ->
                                val seekPos = (ratio * totalDuration).toLong()
                                exoPlayer.seekTo(seekPos)
                                currentPosition = seekPos
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NetflixColors.NetflixRed,
                                activeTrackColor = NetflixColors.NetflixRed,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Time Row: Elapsed vs Remaining
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color(0xFFCCCCCC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        val remainingMs = (totalDuration - currentPosition).coerceAtLeast(0L)
                        Text(
                            text = "-${formatTime(remainingMs)}",
                            color = Color(0xFF999999),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
