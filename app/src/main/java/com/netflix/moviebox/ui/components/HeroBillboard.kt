package com.netflix.moviebox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.netflix.moviebox.api.MovieBoxItem
import com.netflix.moviebox.ui.theme.NetflixColors
import com.netflix.moviebox.ui.theme.NetflixDimens

@Composable
fun HeroBillboard(
    movie: MovieBoxItem?,
    onPlayClick: (MovieBoxItem) -> Unit,
    onDetailsClick: (MovieBoxItem) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (movie == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(NetflixDimens.HeroHeight)
                .shimmer()
        )
        return
    }

    var isInList by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NetflixDimens.HeroHeight)
    ) {
        // 1. Poster Backdrop (prefer wide stills backdrop)
        AsyncImage(
            model = movie.stillsUrl ?: movie.coverUrl,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Multi-stop Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            NetflixColors.BackgroundDark
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 3. Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "N",
                color = NetflixColors.NetflixRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NetflixColors.PureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 4. Hero Information & Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language & Score Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = NetflixColors.NetflixRed,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = movie.detectedLang.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${movie.score} Match",
                    color = Color(0xFF46D369),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = movie.releaseDate,
                    color = NetflixColors.TextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = movie.title,
                color = NetflixColors.PureWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: My List | Play | Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // My List with Lottie morph
                LottieMyListButton(
                    isInList = isInList,
                    onToggle = { isInList = it }
                )

                // Play Button
                Button(
                    onClick = { onPlayClick(movie) },
                    colors = ButtonDefaults.buttonColors(containerColor = NetflixColors.PureWhite),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .width(115.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Info Action
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onDetailsClick(movie) }
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = NetflixColors.PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Info",
                        color = NetflixColors.PureWhite,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
