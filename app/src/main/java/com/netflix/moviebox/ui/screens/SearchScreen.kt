package com.netflix.moviebox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    var searchResults by remember { mutableStateOf<List<MovieBoxItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var trendingKeywords by remember { mutableStateOf<List<String>>(emptyList()) }

    val categories = listOf("All", "Movies", "TV Shows", "Anime")

    val subjectType = when (selectedCategory) {
        "Movies" -> 1
        "TV Shows" -> 2
        "Anime" -> 6
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
                "Anime" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(genre = "Anime", perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Anime", subjectType = 6, maxPages = 2)
                }
                "Movies" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(subjectType = 1, perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Marvel", subjectType = 1, maxPages = 2)
                }
                "TV Shows" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(subjectType = 2, perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Series", subjectType = 2, maxPages = 2)
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
        // 1. Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search movies, TV shows, anime...", color = NetflixColors.TextSecondary, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixColors.TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = NetflixColors.TextMuted)
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = NetflixColors.CardSurface,
                unfocusedContainerColor = NetflixColors.CardSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )

        // 2. Category Filter Chips (All, Movies, TV Shows, Anime)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = (selectedCategory == cat),
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NetflixColors.NetflixRed,
                        selectedLabelColor = Color.White,
                        containerColor = NetflixColors.CardSurface,
                        labelColor = NetflixColors.TextMuted
                    )
                )
            }
        }

        // 3. Trending Searches ("Everyone is searching") Chips
        if (searchQuery.isEmpty() && trendingKeywords.isNotEmpty()) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF00D26A),
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

        // 4. Results Grid with Authentic MovieBox Cards
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NetflixColors.NetflixRed)
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No results found for \"$searchQuery\"",
                    color = NetflixColors.TextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
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
                .clip(RoundedCornerShape(6.dp))
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
                color = Color(0xFF00D26A),
                shape = RoundedCornerShape(bottomEnd = 6.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "Limited Free",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            // Bottom-right: Rating Badge
            if (movie.score > 0.0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
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
            color = Color(0x1A00D26A),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, Color(0xFF00D26A)),
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
                    tint = Color(0xFF00D26A),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Watch now",
                    color = Color(0xFF00D26A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
