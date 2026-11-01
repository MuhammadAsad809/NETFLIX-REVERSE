package com.netflix.moviebox

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netflix.moviebox.api.MovieBoxClient
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.downloader.MovieDownloader
import com.netflix.moviebox.ui.components.DetailBottomSheet
import com.netflix.moviebox.ui.player.PlayerActivity
import com.netflix.moviebox.ui.screens.DownloadsScreen
import com.netflix.moviebox.ui.screens.HomeScreen
import com.netflix.moviebox.ui.screens.SearchScreen
import com.netflix.moviebox.ui.screens.SplashScreen
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixTheme
import kotlinx.coroutines.launch

enum class AppNavTab {
    HOME,
    SEARCH,
    DOWNLOADS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            NetflixTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    MainAppScaffold()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold() {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppNavTab.HOME) }
    var selectedMovie by remember { mutableStateOf<MovieBoxItem?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NetflixColors.BottomNavBackground,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = (currentTab == AppNavTab.HOME),
                    onClick = { currentTab = AppNavTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetflixColors.NavItemActive,
                        selectedTextColor = NetflixColors.NavItemActive,
                        unselectedIconColor = NetflixColors.NavItemInactive,
                        unselectedTextColor = NetflixColors.NavItemInactive,
                        indicatorColor = NetflixColors.BottomNavBackground
                    )
                )
                NavigationBarItem(
                    selected = (currentTab == AppNavTab.SEARCH),
                    onClick = { currentTab = AppNavTab.SEARCH },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetflixColors.NavItemActive,
                        selectedTextColor = NetflixColors.NavItemActive,
                        unselectedIconColor = NetflixColors.NavItemInactive,
                        unselectedTextColor = NetflixColors.NavItemInactive,
                        indicatorColor = NetflixColors.BottomNavBackground
                    )
                )
                NavigationBarItem(
                    selected = (currentTab == AppNavTab.DOWNLOADS),
                    onClick = { currentTab = AppNavTab.DOWNLOADS },
                    icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
                    label = { Text("Downloads", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetflixColors.NavItemActive,
                        selectedTextColor = NetflixColors.NavItemActive,
                        unselectedIconColor = NetflixColors.NavItemInactive,
                        unselectedTextColor = NetflixColors.NavItemInactive,
                        indicatorColor = NetflixColors.BottomNavBackground
                    )
                )
            }
        },
        containerColor = NetflixColors.PureBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                AppNavTab.HOME -> {
                    HomeScreen(
                        onMovieClick = { movie -> selectedMovie = movie },
                        onPlayMovie = { movie ->
                            coroutineScope.launch {
                                val isSeries = movie.subjectType == 2
                                val playRes = MovieBoxClient.getEpisodePlayStream(
                                    subjectId = movie.subjectId,
                                    se = if (isSeries) 1 else 0,
                                    ep = if (isSeries) 1 else 0,
                                    detailPath = movie.detailPath
                                )
                                val bestUrl = playRes.bestStreamUrl
                                    ?: MovieBoxClient.extractStream(movie.subjectId).fullStreamUrl
                                    ?: movie.trailerUrl
                                if (!bestUrl.isNullOrBlank()) {
                                    val titleBadge = if (isSeries) "${movie.title} - S1:E1" else "${movie.title} [Full Movie]"
                                    PlayerActivity.launch(
                                        context = context,
                                        streamUrl = bestUrl,
                                        title = titleBadge,
                                        subjectId = movie.subjectId,
                                        seasonNumber = if (isSeries) 1 else 0,
                                        episodeNumber = if (isSeries) 1 else 0,
                                        totalEpisodes = 12,
                                        detailPath = movie.detailPath
                                    )
                                } else {
                                    Toast.makeText(context, "Stream not available", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSearchClick = { currentTab = AppNavTab.SEARCH }
                    )
                }
                AppNavTab.SEARCH -> {
                    SearchScreen(
                        onMovieClick = { movie -> selectedMovie = movie }
                    )
                }
                AppNavTab.DOWNLOADS -> {
                    DownloadsScreen(
                        onPlayOfflineMovie = { filePath, title ->
                            PlayerActivity.launch(context, filePath, title)
                        }
                    )
                }
            }

            // Global Detail BottomSheet (Safe composable invocation)
            val currentMovie = selectedMovie
            if (currentMovie != null) {
                DetailBottomSheet(
                    movie = currentMovie,
                    onDismiss = { selectedMovie = null },
                    onPlayClick = { streamUrl, title, subId, se, ep, totalEps, dPath ->
                        selectedMovie = null
                        PlayerActivity.launch(
                            context = context,
                            streamUrl = streamUrl,
                            title = title,
                            subjectId = subId,
                            seasonNumber = se,
                            episodeNumber = ep,
                            totalEpisodes = totalEps,
                            detailPath = dPath
                        )
                    },
                    onDownloadClick = { subjectId, title ->
                        coroutineScope.launch {
                            val isSeries = currentMovie.subjectType == 2
                            val playRes = MovieBoxClient.getEpisodePlayStream(
                                subjectId = subjectId,
                                se = if (isSeries) 1 else 0,
                                ep = if (isSeries) 1 else 0,
                                detailPath = currentMovie.detailPath
                            )
                            val resolvedDownloadUrl = playRes.bestStreamUrl ?: MovieBoxClient.extractStream(subjectId).fullStreamUrl
                            val queued = MovieDownloader.startDownload(context, subjectId, title, directDownloadUrl = resolvedDownloadUrl)
                            if (queued) {
                                Toast.makeText(context, "Download started for $title", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Unable to resolve download link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
