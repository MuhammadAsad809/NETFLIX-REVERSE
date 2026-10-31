package com.netflix.moviebox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.netflix.moviebox.downloader.MovieDownloader
import com.netflix.moviebox.ui.theme.NetflixColors
import java.io.File

@Composable
fun DownloadsScreen(
    onPlayOfflineMovie: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTasks by MovieDownloader.downloadTasks.collectAsState()
    var completedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(Unit) {
        completedFiles = MovieDownloader.getCompletedDownloads()
    }

    val nuxComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottiefiles/downloads_nux.json")
    )
    val nuxProgress by animateLottieCompositionAsState(
        composition = nuxComposition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixColors.PureBlack)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Downloads",
            color = NetflixColors.PureWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (activeTasks.isEmpty() && completedFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LottieAnimation(
                        composition = nuxComposition,
                        progress = { nuxProgress },
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Movies you download will appear here.",
                        color = NetflixColors.TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Active Downloads
                items(activeTasks) { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NetflixColors.CardSurface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = task.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                color = NetflixColors.NetflixRed,
                                trackColor = Color(0xFF333333),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Downloading to external storage...",
                                color = NetflixColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 2. Completed Offline Files
                items(completedFiles) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NetflixColors.CardSurface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlayOfflineMovie(file.absolutePath, file.nameWithoutExtension)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = NetflixColors.NetflixRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.nameWithoutExtension,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${file.length() / (1024 * 1024)} MB • Ready for Offline Playback",
                                    color = NetflixColors.TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = {
                                onPlayOfflineMovie(file.absolutePath, file.nameWithoutExtension)
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
