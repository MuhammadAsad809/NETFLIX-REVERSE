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
                                val stream = MovieBoxClient.extractStream(movie.subjectId)
                                val url = stream.fullStreamUrl ?: stream.trailerUrl ?: movie.trailerUrl
                                if (url != null) {
                                    PlayerActivity.launch(context, url, movie.title)
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
                    onPlayClick = { streamUrl, title ->
                        selectedMovie = null
                        PlayerActivity.launch(context, streamUrl, title)
                    },
                    onDownloadClick = { subjectId, title ->
                        coroutineScope.launch {
                            val queued = MovieDownloader.startDownload(context, subjectId, title)
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
