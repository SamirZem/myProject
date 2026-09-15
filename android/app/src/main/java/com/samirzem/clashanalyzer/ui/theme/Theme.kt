package com.samirzem.clashanalyzer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElixirPurple = Color(0xFFB03DF0)
private val ElixirPurpleDark = Color(0xFF7B2CB0)
private val GoodGreen = Color(0xFF4CD964)
private val WarnAmber = Color(0xFFF5A623)
private val BadRed = Color(0xFFE0483E)

val GoodColor = GoodGreen
val MinorColor = WarnAmber
val MajorColor = BadRed

private val LightColors = lightColorScheme(
    primary = ElixirPurple,
    secondary = ElixirPurpleDark,
)

private val DarkColors = darkColorScheme(
    primary = ElixirPurple,
    secondary = ElixirPurpleDark,
)

@Composable
fun ClashAnalyzerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
