package com.unam.dora.ui.theme

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

private val HackerColorScheme = darkColorScheme(
    primary = Color(0xFF00FF00),          // Matrix-Grün
    secondary = Color(0xFF008F11),        // Dunkleres Grün
    tertiary = Color(0xFF00B386),         // Cyan-Grün
    background = Color(0xFF000000),       // Reines Schwarz
    surface = Color(0xFF0A0A0A),          // Fast Schwarz
    error = Color(0xFFFF0000),            // Hackerrot
    onPrimary = Color(0xFF000000),        // Schwarz für Text auf Grün
    onSecondary = Color(0xFF000000),      // Schwarz für Text auf Grün
    onTertiary = Color(0xFF000000),       // Schwarz für Text auf Grün
    onBackground = Color(0xFF00FF00),     // Matrix-Grün für Text
    onSurface = Color(0xFF00FF00)         // Matrix-Grün für Text
)

@Composable
fun ThemeDark(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = HackerColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color(0xFF000000).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}