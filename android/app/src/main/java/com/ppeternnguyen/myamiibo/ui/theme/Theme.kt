package com.ppeternnguyen.myamiibo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Brand colors shared with the web and iOS apps. */
object AmiiboColors {
    val Owned = Color(0xFF16A34A)
    val Wishlist = Color(0xFFD97706)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFFE60012),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE8EA),
    onPrimaryContainer = Color(0xFF5C0008),
    secondary = Color(0xFF63636E),
    secondaryContainer = Color(0xFFFDE8EA),
    onSecondaryContainer = Color(0xFF5C0008),
    background = Color(0xFFF7F7F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F2),
    surfaceContainer = Color(0xFFF3F3F5),
    surfaceContainerHigh = Color(0xFFEDEDF0),
    surfaceContainerHighest = Color(0xFFE7E7EB),
    outlineVariant = Color(0xFFE3E3E8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF4655),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1418),
    onPrimaryContainer = Color(0xFFFFDADC),
    secondary = Color(0xFFA0A0AB),
    secondaryContainer = Color(0xFF3A1418),
    onSecondaryContainer = Color(0xFFFFDADC),
    background = Color(0xFF0E0E11),
    surface = Color(0xFF18181C),
    surfaceVariant = Color(0xFF222228),
    surfaceContainer = Color(0xFF1B1B20),
    surfaceContainerHigh = Color(0xFF24242A),
    surfaceContainerHighest = Color(0xFF2C2C33),
    outlineVariant = Color(0xFF2E2E36),
)

@Composable
fun MyAmiiboTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
