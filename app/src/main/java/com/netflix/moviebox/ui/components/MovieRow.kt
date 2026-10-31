package com.netflix.moviebox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Locale

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
    Box(
        modifier = Modifier
            .width(NetflixDimens.PosterWidth)
            .height(NetflixDimens.PosterHeight)
            .clip(RoundedCornerShape(NetflixDimens.PosterCornerRadius))
            .background(NetflixColors.CardSurface)
            .clickable(onClick = onClick)
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
            shape = RoundedCornerShape(bottomEnd = 4.dp),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = "Limited Free",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Bottom-right: Rating Badge
        if (movie.score > 0.0) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(topStart = 4.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", movie.score),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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

        Box(
            modifier = Modifier
                .width(NetflixDimens.Top10PosterWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(NetflixDimens.PosterCornerRadius))
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
                shape = RoundedCornerShape(bottomEnd = 4.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "Limited Free",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Bottom-right: Rating Badge
            if (movie.score > 0.0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(topStart = 4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", movie.score),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
