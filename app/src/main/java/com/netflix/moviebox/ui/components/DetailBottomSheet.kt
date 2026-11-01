package com.netflix.moviebox.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomSheet(
    movie: MovieBoxItem,
    onDismiss: () -> Unit,
    onPlayClick: (streamUrl: String, title: String) -> Unit,
    onDownloadClick: (subjectId: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Current movie release (switchable via language selector or recommendations)
    var currentMovie by remember(movie.subjectId) { mutableStateOf(movie) }
    var languageReleases by remember { mutableStateOf<List<MovieBoxItem>>(emptyList()) }
    var recommendations by remember { mutableStateOf<List<MovieBoxItem>>(emptyList()) }
    var detailResult by remember { mutableStateOf<SubjectDetailResult?>(null) }
    var isLoadingDetail by remember { mutableStateOf(true) }
    var selectedSeasonIndex by remember { mutableIntStateOf(0) }
    var selectedQuality by remember { mutableStateOf("1080p") }
    var isMyListAdded by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var isResolvingPlay by remember { mutableStateOf(false) }
    var resolvingEpisodeNumber by remember { mutableStateOf<Int?>(null) }

    // Fetch movie details and recommendations on currentMovie change
    LaunchedEffect(currentMovie.subjectId) {
        isLoadingDetail = true
        selectedSeasonIndex = 0
        detailResult = MovieBoxClient.getSubjectDetail(currentMovie.subjectId)
        recommendations = MovieBoxClient.getDetailRecommendations(currentMovie.subjectId)
        isLoadingDetail = false
    }

    // Fetch related audio releases on currentMovie change
    LaunchedEffect(currentMovie.title) {
        languageReleases = MovieBoxClient.getRelatedLanguageReleases(currentMovie.title)
    }

    val seasons = detailResult?.seasons?.filter { it.maxEpisodes > 0 } ?: emptyList()
    val isSeries = (currentMovie.subjectType == 2 || seasons.isNotEmpty()) && seasons.any { it.maxEpisodes > 0 }
    val activeSeason: SeasonDetail? = seasons.getOrNull(selectedSeasonIndex) ?: seasons.firstOrNull()
    val activeDetailPath = detailResult?.detailPath?.ifBlank { currentMovie.detailPath } ?: currentMovie.detailPath

    // Play helper function
    val startPlayback: () -> Unit = {
        coroutineScope.launch {
            isResolvingPlay = true
            try {
                val seNum = if (isSeries) (activeSeason?.seasonNumber ?: 1) else 0
                val epNum = if (isSeries) (activeSeason?.episodes?.firstOrNull()?.episodeNumber ?: 1) else 0
                val playRes = MovieBoxClient.getEpisodePlayStream(
                    subjectId = currentMovie.subjectId,
                    se = seNum,
                    ep = epNum,
                    detailPath = activeDetailPath
                )
                val matched = playRes.streams.firstOrNull {
                    it.resolution.contains(selectedQuality.replace("p", ""))
                } ?: playRes.streams.firstOrNull()

                val staticStreamUrl = detailResult?.streams?.fullStreamUrl ?: detailResult?.streams?.trailerUrl ?: currentMovie.trailerUrl
                val finalUrl = matched?.url ?: staticStreamUrl
                if (finalUrl != null) {
                    val playTitle = if (isSeries) {
                        "${currentMovie.title} - S${seNum}:E${epNum} [${selectedQuality}]"
                    } else {
                        "${currentMovie.title} [${selectedQuality}]"
                    }
                    onPlayClick(finalUrl, playTitle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isResolvingPlay = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF444444))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Cinematic Header Banner with Gradient & Play Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1F1F1F))
                    .clickable { startPlayback() }
            ) {
                AsyncImage(
                    model = currentMovie.stillsUrl ?: currentMovie.coverUrl,
                    contentDescription = currentMovie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color(0xFF141414)
                                )
                            )
                        )
                )

                // Central Play Circle Button
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, NetflixColors.MovieBoxGreen),
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = NetflixColors.MovieBoxGreen,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(start = 2.dp)
                            .align(Alignment.Center)
                    )
                }

                // Top-Left: Limited Free badge
                Surface(
                    color = NetflixColors.MovieBoxGreen,
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Limited Free",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Audio & Video Badges Top-Right
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
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

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Movie Title
            Text(
                text = currentMovie.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata Row (Rating, Year, Duration, Language, 1080P)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // MovieBox Green Rating Chip
                Surface(
                    color = Color(0x2600D26A),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, NetflixColors.MovieBoxGreen)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", currentMovie.score),
                            color = NetflixColors.MovieBoxGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = currentMovie.releaseDate,
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = currentMovie.duration,
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Surface(
                    color = Color(0xFF262626),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = selectedQuality.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = Color(0xFF262626),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = currentMovie.detectedLang.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                if (isSeries) {
                    Surface(
                        color = NetflixColors.MovieBoxGreen,
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "TV SERIES",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Primary MovieBox Solid Green "Watch Now" Button
            Button(
                onClick = { startPlayback() },
                enabled = !isLoadingDetail && !isResolvingPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetflixColors.MovieBoxGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                if (isLoadingDetail || isResolvingPlay) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isResolvingPlay) "Loading Stream ($selectedQuality)..." else "Resolving Stream...",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isSeries -> "▶ Play Series ($selectedQuality)"
                            else -> "▶ Watch Now ($selectedQuality)"
                        },
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. MovieBox 4-Action Quick Toolbar (Download, My List, Rate, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Download
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val downloadTitle = "${currentMovie.title}_${selectedQuality}"
                        onDownloadClick(currentMovie.subjectId, downloadTitle)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Download", color = Color(0xFFCCCCCC), fontSize = 11.sp)
                }

                // My List
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        isMyListAdded = !isMyListAdded
                        Toast.makeText(
                            context,
                            if (isMyListAdded) "Added to My List" else "Removed from My List",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(
                        imageVector = if (isMyListAdded) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "My List",
                        tint = if (isMyListAdded) NetflixColors.MovieBoxGreen else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "My List",
                        color = if (isMyListAdded) NetflixColors.MovieBoxGreen else Color(0xFFCCCCCC),
                        fontSize = 11.sp
                    )
                }

                // Rate
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        isLiked = !isLiked
                        Toast.makeText(
                            context,
                            if (isLiked) "Rated: Loved it!" else "Rating cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Rate",
                        tint = if (isLiked) NetflixColors.MovieBoxGreen else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rate",
                        color = if (isLiked) NetflixColors.MovieBoxGreen else Color(0xFFCCCCCC),
                        fontSize = 11.sp
                    )
                }

                // Share
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Watch \"${currentMovie.title}\" on MovieBox! High quality free streaming available."
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share with"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Share", color = Color(0xFFCCCCCC), fontSize = 11.sp)
                }
            }

            // 5. Audio Languages Selector (Switches between Hindi, English, Urdu, Dual Audio)
            if (languageReleases.size > 1) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🎧 Available Audio Languages",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(languageReleases) { rel ->
                        val isSelected = rel.subjectId == currentMovie.subjectId
                        Surface(
                            color = if (isSelected) NetflixColors.MovieBoxGreen else Color(0xFF222222),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                0.5.dp,
                                if (isSelected) NetflixColors.MovieBoxGreen else Color(0xFF333333)
                            ),
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
                                color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Video Quality Selector (1080p, 720p, 480p)
            Text(
                text = "Stream & Download Quality",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val availableQualities = listOf("1080p", "720p", "480p")
                availableQualities.forEach { q ->
                    val isSelected = selectedQuality == q
                    Surface(
                        color = if (isSelected) NetflixColors.MovieBoxGreen else Color(0xFF222222),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            0.5.dp,
                            if (isSelected) NetflixColors.MovieBoxGreen else Color(0xFF333333)
                        ),
                        modifier = Modifier.clickable { selectedQuality = q }
                    ) {
                        Text(
                            text = q,
                            color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 7. TV Series Episode Selection
            if (isSeries && activeSeason != null && activeSeason.episodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                // Season Selector
                if (seasons.size > 1) {
                    Text(
                        text = "Seasons",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(seasons.size) { idx ->
                            val se = seasons[idx]
                            val isSelected = selectedSeasonIndex == idx
                            Surface(
                                color = if (isSelected) NetflixColors.MovieBoxGreen else Color(0xFF222222),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isSelected) NetflixColors.MovieBoxGreen else Color(0xFF333333)
                                ),
                                modifier = Modifier.clickable { selectedSeasonIndex = idx }
                            ) {
                                Text(
                                    text = "Season ${se.seasonNumber} (${se.maxEpisodes} eps)",
                                    color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = "Episodes (${activeSeason.episodes.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Episodes List
                activeSeason.episodes.forEach { ep ->
                    EpisodeRowItem(
                        episode = ep,
                        seasonNumber = activeSeason.seasonNumber,
                        movieTitle = currentMovie.title,
                        selectedQuality = selectedQuality,
                        isResolving = resolvingEpisodeNumber == ep.episodeNumber,
                        onPlayEpisode = { seNum, epNum ->
                            coroutineScope.launch {
                                resolvingEpisodeNumber = epNum
                                try {
                                    val playRes = MovieBoxClient.getEpisodePlayStream(
                                        subjectId = currentMovie.subjectId,
                                        se = seNum,
                                        ep = epNum,
                                        detailPath = activeDetailPath
                                    )
                                    val matched = playRes.streams.firstOrNull {
                                        it.resolution.contains(selectedQuality.replace("p", ""))
                                    } ?: playRes.streams.firstOrNull()

                                    val staticStreamUrl = detailResult?.streams?.fullStreamUrl ?: currentMovie.trailerUrl
                                    val finalUrl = matched?.url ?: staticStreamUrl
                                    if (finalUrl != null) {
                                        val epTitle = "${currentMovie.title} - S${seNum}:E${epNum} [${selectedQuality}]"
                                        onPlayClick(finalUrl, epTitle)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    resolvingEpisodeNumber = null
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // 8. Synopsis
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Synopsis",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentMovie.description.ifEmpty { "High-speed streaming available via MovieBox CDN with multi-language audio selection." },
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // 9. More Like This (Related Titles)
            if (recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "More Like This",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recommendations) { recItem ->
                        Column(
                            modifier = Modifier
                                .width(105.dp)
                                .clickable {
                                    currentMovie = recItem
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(105.dp)
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF222222))
                            ) {
                                AsyncImage(
                                    model = recItem.coverUrl,
                                    contentDescription = recItem.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    color = NetflixColors.MovieBoxGreen,
                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "Limited Free",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                if (recItem.score > 0.0) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(topStart = 6.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB800),
                                                modifier = Modifier.size(9.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.1f", recItem.score),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = recItem.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

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
    isResolving: Boolean,
    onPlayEpisode: (se: Int, ep: Int) -> Unit
) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color(0xFF2E2E2E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isResolving) {
                onPlayEpisode(seasonNumber, episode.episodeNumber)
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
                    .size(width = 85.dp, height = 52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = episode.stillUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isResolving) {
                    CircularProgressIndicator(
                        color = NetflixColors.MovieBoxGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                } else {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Episode",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${episode.duration} • $selectedQuality",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = NetflixColors.MovieBoxGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
