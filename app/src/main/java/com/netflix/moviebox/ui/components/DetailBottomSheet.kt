package com.netflix.moviebox.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
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
import com.netflix.moviebox.api.EpisodeItem
import com.netflix.moviebox.api.MovieBoxClient
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.api.SeasonDetail
import com.netflix.moviebox.api.SubjectDetailResult
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

    // Current movie release (switchable via language selector)
    var currentMovie by remember(movie.subjectId) { mutableStateOf(movie) }
    var languageReleases by remember { mutableStateOf<List<MovieBoxItem>>(emptyList()) }
    var detailResult by remember { mutableStateOf<SubjectDetailResult?>(null) }
    var isLoadingDetail by remember { mutableStateOf(true) }
    var selectedSeasonIndex by remember { mutableIntStateOf(0) }
    var selectedQuality by remember { mutableStateOf("1080p") }

    // Fetch movie details on currentMovie change
    LaunchedEffect(currentMovie.subjectId) {
        isLoadingDetail = true
        detailResult = MovieBoxClient.getSubjectDetail(currentMovie.subjectId)
        isLoadingDetail = false
    }

    // Fetch related audio releases on currentMovie change
    LaunchedEffect(currentMovie.title) {
        languageReleases = MovieBoxClient.getRelatedLanguageReleases(currentMovie.title)
    }

    val seasons = detailResult?.seasons?.filter { it.maxEpisodes > 0 } ?: emptyList()
    val isSeries = (currentMovie.subjectType == 2 || seasons.isNotEmpty()) && seasons.any { it.maxEpisodes > 0 }
    val activeSeason: SeasonDetail? = seasons.getOrNull(selectedSeasonIndex) ?: seasons.firstOrNull()

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
                    model = currentMovie.stillsUrl ?: currentMovie.coverUrl,
                    contentDescription = currentMovie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Title & Metadata
            Text(
                text = currentMovie.title,
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
                    text = "${currentMovie.score} Match",
                    color = Color(0xFF46D369),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentMovie.releaseDate,
                    color = NetflixColors.TextMuted,
                    fontSize = 13.sp
                )
                Surface(
                    color = Color(0xFF333333),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = currentMovie.detectedLang.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isSeries) {
                    Surface(
                        color = NetflixColors.NetflixRed,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "TV SERIES",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
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

            // 3. Audio Languages Selector (Switches between Hindi, English, Urdu, Dual Audio)
            if (languageReleases.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "🎧 Available Audio Languages",
                    color = NetflixColors.PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(languageReleases) { rel ->
                        val isSelected = rel.subjectId == currentMovie.subjectId
                        Surface(
                            color = if (isSelected) NetflixColors.NetflixRed else Color(0xFF262626),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable {
                                currentMovie = rel
                            }
                        ) {
                            val flag = when (rel.detectedLang) {
                                "Hindi" -> "🇮🇳 "
                                "English" -> "🇬🇧 "
                                "Urdu" -> "🇵🇰 "
                                "Dual Audio" -> "🔀 "
                                "Punjabi" -> "👳 "
                                else -> "🌐 "
                            }
                            Text(
                                text = flag + rel.detectedLang,
                                color = if (isSelected) Color.White else NetflixColors.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Video Quality Selector
            Text(
                text = "Stream & Download Quality",
                color = NetflixColors.PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val availableQualities = listOf("1080p", "720p", "480p")
                availableQualities.forEach { q ->
                    val isSelected = selectedQuality == q
                    Surface(
                        color = if (isSelected) NetflixColors.NetflixRed else Color(0xFF262626),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { selectedQuality = q }
                    ) {
                        Text(
                            text = q,
                            color = if (isSelected) Color.White else NetflixColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Play Button (Full Movie or First Episode)
            val hasFull = detailResult?.streams?.hasFullMovie == true
            val streamUrl = if (hasFull) detailResult?.streams?.fullStreamUrl else (detailResult?.streams?.trailerUrl ?: detailResult?.trailerUrl)

            Button(
                onClick = {
                    if (streamUrl != null) {
                        val playTitle = "${currentMovie.title} [${selectedQuality}]"
                        onPlayClick(streamUrl, playTitle)
                    }
                },
                enabled = !isLoadingDetail && streamUrl != null,
                colors = ButtonDefaults.buttonColors(containerColor = NetflixColors.PureWhite),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                if (isLoadingDetail) {
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
                        text = when {
                            hasFull -> "▶️ Play Full Movie ($selectedQuality)"
                            isSeries -> "▶️ Play Series ($selectedQuality)"
                            else -> "🎬 Watch Trailer ($selectedQuality)"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Download Button
            Button(
                onClick = {
                    val downloadTitle = "${currentMovie.title}_${selectedQuality}"
                    onDownloadClick(currentMovie.subjectId, downloadTitle)
                },
                enabled = hasFull,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = NetflixColors.PureWhite)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasFull) "⬇️ Download Full Movie ($selectedQuality)" else "⬇️ Download Unavailable",
                    color = NetflixColors.PureWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. TV Series Seasons & Episodes (ONLY if series with genuine seasons)
            if (isSeries && seasons.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Episodes (${activeSeason?.episodes?.size ?: 0})",
                    color = NetflixColors.PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Season Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    seasons.forEachIndexed { idx, s ->
                        val isSeasonSelected = idx == selectedSeasonIndex
                        Surface(
                            color = if (isSeasonSelected) NetflixColors.NetflixRed else Color(0xFF1E1E1E),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { selectedSeasonIndex = idx }
                        ) {
                            Text(
                                text = "Season ${s.seasonNumber}",
                                color = if (isSeasonSelected) NetflixColors.PureWhite else NetflixColors.TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (isSeasonSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Real Episode List with genuine durations
                val episodeList = activeSeason?.episodes ?: emptyList()
                episodeList.forEach { ep ->
                    EpisodeRowItem(
                        episode = ep,
                        seasonNumber = activeSeason?.seasonNumber ?: 1,
                        movieTitle = currentMovie.title,
                        selectedQuality = selectedQuality,
                        fallbackStreamUrl = streamUrl,
                        onPlayClick = onPlayClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 8. Synopsis
            Text(
                text = currentMovie.description.ifEmpty { "High-speed streaming available via MovieBox CDN with multi-language audio selection." },
                color = NetflixColors.TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EpisodeRowItem(
    episode: EpisodeItem,
    seasonNumber: Int,
    movieTitle: String,
    selectedQuality: String,
    fallbackStreamUrl: String?,
    onPlayClick: (streamUrl: String, title: String) -> Unit
) {
    Surface(
        color = Color(0xFF1F1F1F),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (fallbackStreamUrl != null) {
                    val epTitle = "$movieTitle - S${seasonNumber}:E${episode.episodeNumber} [${selectedQuality}]"
                    onPlayClick(fallbackStreamUrl, epTitle)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
                    .background(Color.Black, shape = RoundedCornerShape(4.dp))
            ) {
                AsyncImage(
                    model = episode.stillUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Play Episode",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.title}",
                    color = NetflixColors.PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${episode.duration} • $selectedQuality",
                    color = NetflixColors.TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
