package com.netflix.moviebox.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.netflix.moviebox.R
import com.netflix.moviebox.api.MovieBoxClient
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.api.StreamResolution
import com.netflix.moviebox.ui.theme.NetflixColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomSheet(
    movie: MovieBoxItem,
    onDismiss: () -> Unit,
    onPlayClick: (streamUrl: String, title: String) -> Unit,
    onDownloadClick: (subjectId: String, title: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var streamResolution by remember { mutableStateOf<StreamResolution?>(null) }
    var isLoadingStream by remember { mutableStateOf(true) }

    LaunchedEffect(movie.subjectId) {
        isLoadingStream = true
        streamResolution = MovieBoxClient.extractStream(movie.subjectId)
        isLoadingStream = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NetflixColors.CardSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF555555))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = movie.coverUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Title & Metadata
            Text(
                text = movie.title,
                color = NetflixColors.PureWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${movie.score} Match",
                    color = Color(0xFF46D369),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = movie.releaseDate,
                    color = NetflixColors.TextMuted,
                    fontSize = 13.sp
                )
                Surface(
                    color = Color(0xFF333333),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = movie.detectedLang.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Extracted Hawkins Audio / Video Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.hawkins_tag_dolby_vision),
                        contentDescription = "Dolby Vision",
                        modifier = Modifier.height(14.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.hawkins_tag_dolby_atmos),
                        contentDescription = "Dolby Atmos",
                        modifier = Modifier.height(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Play Button (Full Movie)
            Button(
                onClick = {
                    val streamUrl = streamResolution?.fullStreamUrl ?: streamResolution?.trailerUrl
                    if (streamUrl != null) {
                        onPlayClick(streamUrl, movie.title)
                    }
                },
                enabled = !isLoadingStream && (streamResolution?.fullStreamUrl != null || streamResolution?.trailerUrl != null),
                colors = ButtonDefaults.buttonColors(containerColor = NetflixColors.PureWhite),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                if (isLoadingStream) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resolving Stream...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (streamResolution?.hasFullMovie == true) "Play Full Movie" else "Play Trailer",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Download Button
            Button(
                onClick = {
                    onDownloadClick(movie.subjectId, movie.title)
                },
                enabled = streamResolution?.hasFullMovie == true,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = NetflixColors.PureWhite)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Download Movie",
                    color = NetflixColors.PureWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Synopsis
            Text(
                text = movie.description.ifEmpty { "High-speed streaming available via MovieBox CDN with multi-language audio selection." },
                color = NetflixColors.TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
