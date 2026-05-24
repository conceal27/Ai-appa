package com.ai.companion.presentation.base

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryGreen = Color(0xFF07C160)
val PrimaryDarkGreen = Color(0xFF06AD56)
val LightGreen = Color(0xFF88E8B3)
val UserBubbleGreen = Color(0xFF95EC69)

val BackgroundWhite = Color(0xFFFFFFFF)
val BackgroundGray = Color(0xFFF5F6F7)
val TextPrimary = Color(0xFF191919)
val TextSecondary = Color(0xFF666666)
val TextHint = Color(0xFF999999)
val DividerColor = Color(0xFFE8E8E8)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    secondary = LightGreen,
    background = BackgroundWhite,
    surface = BackgroundWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundGray,
    outline = DividerColor
)

@Composable
fun AICompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
