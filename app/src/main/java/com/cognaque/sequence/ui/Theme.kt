package com.cognaque.sequence.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cognaque.sequence.data.AppColors

// Dark Colors (Original)
val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    secondary = AppColors.Secondary,
    tertiary = AppColors.Tertiary,
    background = AppColors.Background,
    surface = AppColors.Surface,
    error = AppColors.Error,
    surfaceVariant = AppColors.SurfaceVariant,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
    onSurfaceVariant = AppColors.TextSecondary,
    tertiaryContainer = AppColors.AddAction // Mapping AddAction to tertiaryContainer
)

// Light Colors (New)
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF388E3C),       // Darker Green
    secondary = Color(0xFF1976D2),     // Darker Blue
    tertiary = Color(0xFFC2185B),      // Darker Pink
    background = Color(0xFFF5F5F5),    // Light Gray
    surface = Color(0xFFFFFFFF),       // White
    error = Color(0xFFD32F2F),         // Red
    surfaceVariant = Color(0xFFEEEEEE), // Very Light Gray
    onBackground = Color(0xFF212121),  // Almost Black
    onSurface = Color(0xFF212121),     // Almost Black
    onSurfaceVariant = Color(0xFF616161), // Dark Gray
    tertiaryContainer = Color(0xFFFFA000) // Darker Amber
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
