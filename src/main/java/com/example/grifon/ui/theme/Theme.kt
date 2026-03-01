package com.example.grifon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GrifonBlue,
    onPrimary = GrifonLightGray,
    secondary = GrifonGold,
    onSecondary = GrifonGray,
    background = GrifonLightGray,
    onBackground = GrifonGray,
    surface = Color.White,
    onSurface = GrifonGray,
)

private val DarkColors = darkColorScheme(
    primary = GrifonGold,
    onPrimary = GrifonBlueDark,
    secondary = GrifonBlue,
    onSecondary = GrifonLightGray,
    background = GrifonGray,
    onBackground = GrifonLightGray,
    surface = GrifonBlueDark,
    onSurface = GrifonLightGray,
)

@Composable
fun GrifonTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = GrifonTypography,
        content = content,
    )
}
