package com.robopal.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrictDarkColorScheme = darkColorScheme(
    primary = NeutralPrimary,
    secondary = NeutralSecondary,
    background = PureBlack,
    surface = DarkSurface,
    error = RobotError,
    onPrimary = PureBlack,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun RoboPalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StrictDarkColorScheme,
        content = content
    )
}
