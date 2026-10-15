package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = DarkGrayBg,
    primaryContainer = SecondaryDark.copy(alpha = 0.3f),
    onPrimaryContainer = TextPrimaryDark,
    secondary = EmeraldGreen,
    onSecondary = DarkGrayBg,
    secondaryContainer = EmeraldGreen.copy(alpha = 0.2f),
    onSecondaryContainer = EmeraldGreen,
    background = DarkGrayBg,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = OutlineDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = CoralError,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = Color(0xFFDBEAFE),      // Soft Blue container
    onPrimaryContainer = Color(0xFF1E40AF),    // Dark Blue text for soft container
    secondary = EmeraldGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = Color(0xFFD1FAE5),    // Soft green container
    onSecondaryContainer = Color(0xFF065F46),  // Dark green text for soft container
    background = LightGrayBg,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),        // Light Slate surface Variant
    onSurfaceVariant = TextSecondary,
    outline = OutlineGray,
    error = CoralError,
    onError = SurfaceWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(
                fontFamily = AppFontFamily
            ),
            content = content
        )
    }
}
