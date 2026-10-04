package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberBlue,
    onPrimary = DarkBackground,
    primaryContainer = CyberIndigo.copy(alpha = 0.3f),
    onPrimaryContainer = TextPrimaryDark,
    secondary = CyberTeal,
    onSecondary = DarkBackground,
    secondaryContainer = CyberTeal.copy(alpha = 0.2f),
    onSecondaryContainer = TextPrimaryDark,
    tertiary = CyberAmber,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkSurfaceHigh,
    error = CyberRose
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightBackground,
    primaryContainer = LightPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = LightBackground,
    background = LightBackground,
    onBackground = DarkSurface,
    surface = LightSurface,
    onSurface = DarkSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = TextMutedDark,
    outline = TextMutedDark.copy(alpha = 0.3f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our sleek cyber theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Provide dark theme experience for AI terminal
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
