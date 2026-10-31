package com.netflix.moviebox.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NetflixColors.NetflixRed,
    secondary = NetflixColors.PureWhite,
    background = NetflixColors.PureBlack,
    surface = NetflixColors.CardSurface,
    onPrimary = NetflixColors.PureWhite,
    onSecondary = NetflixColors.PureBlack,
    onBackground = NetflixColors.PureWhite,
    onSurface = NetflixColors.PureWhite
)

@Composable
fun NetflixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
