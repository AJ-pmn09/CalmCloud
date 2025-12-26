package com.example.calmcloud.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light theme - Engaging K-12 colors with Midnaigle blue
private val LightColorScheme = lightColorScheme(
    primary = CalmBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = CalmBlueLight,
    onPrimaryContainer = CalmBlueDark,
    
    secondary = CalmPurple,
    onSecondary = TextOnPrimary,
    secondaryContainer = CalmPurpleLight,
    onSecondaryContainer = CalmPurple,
    
    tertiary = CalmGreen,
    onTertiary = TextOnPrimary,
    tertiaryContainer = CalmGreenLight,
    onTertiaryContainer = CalmGreen,
    
    error = Color(0xFFD32F2F),
    onError = TextOnPrimary,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),
    
    background = BackgroundWhite,
    onBackground = TextPrimary,
    
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0),
    
    scrim = Color(0xFF000000),
    inverseSurface = DarkSurface,
    inverseOnSurface = TextOnPrimary,
    inversePrimary = CalmBlueLight,
    
    surfaceTint = CalmBlue
)

// Dark theme - Midnaigle dark blue inspired
private val DarkColorScheme = darkColorScheme(
    primary = CalmBlueLight,
    onPrimary = DarkBackground,
    primaryContainer = CalmBlueDark,
    onPrimaryContainer = CalmBlueLight,
    
    secondary = CalmPurpleLight,
    onSecondary = DarkBackground,
    secondaryContainer = CalmPurple,
    onSecondaryContainer = CalmPurpleLight,
    
    tertiary = CalmGreenLight,
    onTertiary = DarkBackground,
    tertiaryContainer = CalmGreen,
    onTertiaryContainer = CalmGreenLight,
    
    error = Color(0xFFEF5350),
    onError = DarkBackground,
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2),
    
    background = DarkBackground,
    onBackground = TextOnPrimary,
    
    surface = DarkSurface,
    onSurface = TextOnPrimary,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF424242),
    
    scrim = Color(0xFF000000),
    inverseSurface = BackgroundWhite,
    inverseOnSurface = TextPrimary,
    inversePrimary = CalmBlue,
    
    surfaceTint = CalmBlueLight
)

@Composable
fun CalmCloudTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color for consistent K-12 branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use WindowInsetsControllerCompat for status bar color
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            // Status bar color is now handled by edge-to-edge and system bars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}