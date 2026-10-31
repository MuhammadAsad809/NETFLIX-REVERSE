package com.netflix.moviebox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.data.HomeCategory
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixDimens

@Composable
fun MovieRow(
    category: HomeCategory,
    onMovieClick: (MovieBoxItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 10.dp)) {
        Text(
            text = category.title,
            color = NetflixColors.PureWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(category.movies) { index, movie ->
                if (category.isRanked) {
                    RankedMovieCard(
                        rank = index + 1,
                        movie = movie,
                        onClick = { onMovieClick(movie) }
                    )
                } else {
                    StandardMovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) }
                    )
                }
            }
        }
    }
}

@Composable
fun StandardMovieCard(
    movie: MovieBoxItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(NetflixDimens.PosterWidth)
            .height(NetflixDimens.PosterHeight)
            .clickable(onClick = onClick),
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

@Composable
fun RankedMovieCard(
    rank: Int,
    movie: MovieBoxItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(NetflixDimens.Top10PosterHeight)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Giant Netflix Number
        Text(
            text = "$rank",
            fontSize = 80.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF555555),
            modifier = Modifier.offset(x = 10.dp)
        )

        Card(
            modifier = Modifier
                .width(NetflixDimens.Top10PosterWidth)
                .fillMaxHeight(),
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
