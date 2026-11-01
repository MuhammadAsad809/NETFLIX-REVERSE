package com.netflix.moviebox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.netflix.moviebox.api.MovieBoxClient
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.ui.theme.NetflixColors
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (MovieBoxItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isListView by remember { mutableStateOf(true) }
    var searchResults by remember { mutableStateOf<List<MovieBoxItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var trendingKeywords by remember { mutableStateOf<List<String>>(emptyList()) }

    // Official MovieBox Category Tabs (matches Image 5)
    val categories = listOf("All", "Movie/Series", "ShortTV", "FightZone", "Sports")

    val subjectType = when (selectedCategory) {
        "Movie/Series" -> 1
        else -> 0
    }

    // Load trending searches on start
    LaunchedEffect(Unit) {
        trendingKeywords = MovieBoxClient.getEveryoneSearch()
    }

    // Debounced Search & Catalog Effect
    LaunchedEffect(searchQuery, selectedCategory) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            delay(300)
            isSearching = true
            searchResults = MovieBoxClient.searchMovies(
                keyword = q,
                subjectType = subjectType,
                maxPages = 4
            )
            isSearching = false
        } else if (q.isEmpty()) {
            isSearching = true
            searchResults = when (selectedCategory) {
                "Movie/Series" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(subjectType = 1, perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Movie", subjectType = 1, maxPages = 2)
                }
                "ShortTV" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(genre = "Short", perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Short", maxPages = 2)
                }
                "FightZone" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(genre = "Action", perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Action", maxPages = 2)
                }
                "Sports" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(genre = "Sport", perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Sports", maxPages = 2)
                }
                else -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(page = 1, perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Trending", maxPages = 3)
                }
            }
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixColors.PureBlack)
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        // 1. MovieBox Search Bar (Image 5 style)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Rounded Pill Search Input
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search movies, series...",
                                    color = Color(0xFF777777),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF999999),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Mic Action Button
            Surface(
                color = Color(0xFF1E1E1E),
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        if (searchQuery.isBlank()) searchQuery = "India"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = NetflixColors.MovieBoxGreen,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(12.dp)
                )
            }
        }

        // 2. MovieBox Category Tabs with Active Underline (Image 5 style)
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFF888888)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(NetflixColors.MovieBoxGreen)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }

        // 3. Trending Searches ("Everyone is searching")
        if (searchQuery.isEmpty() && trendingKeywords.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = NetflixColors.MovieBoxGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Everyone is searching",
                        color = Color(0xFFE5E5E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trendingKeywords) { kw ->
                        Surface(
                            color = Color(0xFF222222),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF333333)),
                            modifier = Modifier.clickable {
                                searchQuery = kw
                            }
                        ) {
                            Text(
                                text = kw,
                                color = Color(0xFFD4D4D4),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Header with Result Count & List/Grid View Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) "Search Results (${searchResults.size})" else "Recommended",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { isListView = !isListView },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isListView) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "Toggle List/Grid View",
                    tint = NetflixColors.MovieBoxGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 5. Results (List View by default matching Image 5, or Grid View matching Image 1 & 3)
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NetflixColors.MovieBoxGreen)
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No results found for \"$searchQuery\"",
                    color = NetflixColors.TextMuted,
                    fontSize = 14.sp
                )
            }
        } else if (isListView) {
            // Authentic MovieBox List View (Image 5)
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { movie ->
                    MovieBoxSearchListItem(
                        movie = movie,
                        onClick = { onMovieClick(movie) }
                    )
                }
            }
        } else {
            // Authentic MovieBox 3-Column Grid View (Image 1 & 3)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { movie ->
                    SearchMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) }
                    )
                }
            }
        }
    }
}

/**
 * Authentic MovieBox List Item (Corresponds directly to Image 5 from the official MovieBox Android APK)
 */
@Composable
fun MovieBoxSearchListItem(
    movie: MovieBoxItem,
    onClick: () -> Unit,
    onPlayClick: () -> Unit = onClick
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Poster on the left (64x92dp, 6dp rounded)
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 92.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222222))
            ) {
                AsyncImage(
                    model = movie.coverUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Limited Free Badge
                Surface(
                    color = NetflixColors.MovieBoxGreen,
                    shape = RoundedCornerShape(bottomEnd = 4.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Free",
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Bottom gradient with language badge (e.g. "Hindi")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = movie.detectedLang,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Middle text column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Meta row: ★ 9.1 | 2024 | India | Comedy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (movie.score > 0.0) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", movie.score),
                            color = Color(0xFFFFB800),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " | ",
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )
                    }

                    val metaParts = mutableListOf<String>()
                    if (movie.releaseDate.isNotBlank()) metaParts.add(movie.releaseDate)
                    if (movie.countryName.isNotBlank()) metaParts.add(movie.countryName)
                    val genreFirst = movie.genre.split(",").firstOrNull()?.trim() ?: ""
                    if (genreFirst.isNotBlank()) metaParts.add(genreFirst)

                    Text(
                        text = metaParts.joinToString(" | ").ifEmpty { movie.detectedLang },
                        color = Color(0xFF999999),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 3. Right circular play button (White circle outline with play icon inside)
            Surface(
                color = Color.Transparent,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, Color.White),
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onPlayClick)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = 2.dp)
                )
            }
        }
    }
}

/**
 * Standard 3-column MovieBox Grid Card (Matches Image 1 & 3 from MovieBox Web)
 */
@Composable
private fun SearchMovieCard(
    movie: MovieBoxItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 1. Poster Image with Limited Free & Rating Badges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NetflixColors.CardSurface)
        ) {
            AsyncImage(
                model = movie.coverUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Top-left: Limited Free Badge
            Surface(
                color = NetflixColors.MovieBoxGreen,
                shape = RoundedCornerShape(bottomEnd = 6.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "Limited Free",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            // Bottom-right: Rating Badge
            if (movie.score > 0.0) {
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
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", movie.score),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 2. Movie Title
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(5.dp))

        // 3. Authentic Green "Watch now" Action Button
        Surface(
            color = NetflixColors.MovieBoxAccentBg,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, NetflixColors.MovieBoxGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NetflixColors.MovieBoxGreen,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Watch now",
                    color = NetflixColors.MovieBoxGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
