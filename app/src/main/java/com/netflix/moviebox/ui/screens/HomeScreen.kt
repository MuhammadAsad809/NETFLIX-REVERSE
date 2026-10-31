package com.netflix.moviebox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.data.HomeCategory
import com.netflix.moviebox.data.MovieBoxRepository
import com.netflix.moviebox.ui.components.HeroBillboard
import com.netflix.moviebox.ui.components.MovieRow
import com.netflix.moviebox.ui.components.ShimmerHeroItem
import com.netflix.moviebox.ui.components.ShimmerPosterItem
import com.netflix.moviebox.ui.theme.NetflixColors

@Composable
fun HomeScreen(
    onMovieClick: (MovieBoxItem) -> Unit,
    onPlayMovie: (MovieBoxItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var heroMovie by remember { mutableStateOf<MovieBoxItem?>(null) }
    var categories by remember { mutableStateOf<List<HomeCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        heroMovie = MovieBoxRepository.getFeaturedHero()
        categories = MovieBoxRepository.getHomeCategories()
        isLoading = false
    }

    if (isLoading) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixColors.BackgroundDark)
        ) {
            ShimmerHeroItem()
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                repeat(3) {
                    ShimmerPosterItem()
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(NetflixColors.BackgroundDark),
            contentPadding = PaddingValues(bottom = 70.dp)
        ) {
            // 1. Hero Billboard Header
            item {
                HeroBillboard(
                    movie = heroMovie,
                    onPlayClick = onPlayMovie,
                    onDetailsClick = onMovieClick,
                    onSearchClick = onSearchClick
                )
            }

            // 2. Dynamic Categories
            items(categories) { category ->
                MovieRow(
                    category = category,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}
