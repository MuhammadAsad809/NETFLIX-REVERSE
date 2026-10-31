package com.netflix.moviebox.ui.screens

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.netflix.moviebox.api.MovieBoxClient
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixDimens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMovieClick: (MovieBoxItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("All") }
    var searchResults by remember { mutableStateOf<List<MovieBoxItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val languages = listOf("All", "Hindi", "Urdu", "Dual Audio", "English")

    // Debounced Search Effect
    LaunchedEffect(searchQuery, selectedLang) {
        if (searchQuery.trim().length >= 2) {
            delay(400)
            isSearching = true
            val lang = if (selectedLang == "All") "" else selectedLang
            searchResults = MovieBoxClient.searchMovies(searchQuery, langFilter = lang)
            isSearching = false
        } else if (searchQuery.isEmpty()) {
            // Default discover results
            isSearching = true
            val lang = if (selectedLang == "All") "" else selectedLang
            searchResults = MovieBoxClient.searchMovies("2024", langFilter = lang)
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NetflixColors.PureBlack)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search movies, genres, Hindi dubbed...", color = NetflixColors.TextSecondary) },
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
                .padding(vertical = 12.dp)
        )

        // 2. Language Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 14.dp)
        ) {
            items(languages) { lang ->
                FilterChip(
                    selected = (selectedLang == lang),
                    onClick = { selectedLang = lang },
                    label = { Text(lang, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NetflixColors.NetflixRed,
                        selectedLabelColor = Color.White,
                        containerColor = NetflixColors.CardSurface,
                        labelColor = NetflixColors.TextMuted
                    )
                )
            }
        }

        // 3. Results Grid
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NetflixColors.NetflixRed)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults) { movie ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NetflixDimens.PosterHeight)
                            .clickable { onMovieClick(movie) },
                        shape = RoundedCornerShape(NetflixDimens.PosterCornerRadius),
                        colors = CardDefaults.cardColors(containerColor = NetflixColors.CardSurface)
                    ) {
                        AsyncImage(
                            model = movie.coverUrl,
                            contentDescription = movie.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
