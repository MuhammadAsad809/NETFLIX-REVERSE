package com.netflix.moviebox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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

    val categories = listOf("All", "Movie", "TV Series", "Anime")

    val subjectType = when (selectedCategory) {
        "Movie" -> 1
        "TV Series" -> 2
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
                "Movie" -> {
                    val filtered = MovieBoxClient.getFilteredCatalog(subjectType = 1, perPage = 30)
                    if (filtered.isNotEmpty()) filtered else MovieBoxClient.searchMovies("Marvel", subjectType = 1, maxPages = 2)
                }
                "TV Series" -> {
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
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixColors.MovieBoxGreen) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = NetflixColors.TextMuted)
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1A1A1A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NetflixColors.MovieBoxGreen,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )

        // 2. Category Filter Chips (All, Movie, TV Series, Anime)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NetflixColors.MovieBoxGreen,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E1E1E),
                        labelColor = Color(0xFFB0B0B0)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color.Transparent,
                        selectedBorderColor = NetflixColors.MovieBoxGreen
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

        // 4. Results Grid with Authentic MovieBox Accurate Card & Grid
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
        } else {
            val isSearchMode = searchQuery.trim().isNotBlank()
            val bestMatch = if (isSearchMode) searchResults.firstOrNull() else null
            val remainingItems = if (isSearchMode && bestMatch != null) searchResults.drop(1) else searchResults

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // If in search mode, display the Accurate Match Hero Card spanning all 3 columns
                if (bestMatch != null) {
                    item(span = { GridItemSpan(3) }) {
                        AccurateMovieMatchCard(
                            movie = bestMatch,
                            onClick = { onMovieClick(bestMatch) }
                        )
                    }

                    if (remainingItems.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            Text(
                                text = "All Results (${searchResults.size})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                    }
                }

                // Grid cards
                items(remainingItems) { movie ->
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
 * Authentic MovieBox "Accurate Match" layout (corresponds to item_search_values_accurate_layout in MovieBox APK)
 */
@Composable
private fun AccurateMovieMatchCard(
    movie: MovieBoxItem,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0x3300D26A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .width(105.dp)
                    .height(148.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262626))
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

                // Rating
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

            Spacer(modifier = Modifier.width(12.dp))

            // Right details column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(148.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        color = Color(0x2600D26A),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "BEST MATCH",
                            color = NetflixColors.MovieBoxGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${movie.releaseDate} • ${movie.duration} • ${movie.detectedLang}",
                        color = NetflixColors.TextMuted,
                        fontSize = 11.sp
                    )

                    if (movie.genre.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = movie.genre.replace(",", " • "),
                            color = Color(0xFF888888),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Solid Green Watch Button
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetflixColors.MovieBoxGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Watch now",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Standard 3-column MovieBox Grid Card
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
