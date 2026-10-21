package com.krunal.rootd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Leaf = Color(0xFF2E7D4F)
private val LeafLight = Color(0xFF7CC59A)
private val Bark = Color(0xFF6D5A45)

private val LightColors = lightColorScheme(
    primary = Leaf,
    onPrimary = Color.White,
    secondary = Bark,
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = LeafLight,
    onPrimary = Color(0xFF00391F),
    secondary = Color(0xFFD7C2A9),
    onSecondary = Color(0xFF3B2E1F),
)

@Composable
fun RootdTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
