package com.example.ui.theme

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElegantLavender,
    secondary = ElegantLavender,
    tertiary = ElegantLavender,
    background = ElegantDarkBackground,
    surface = ElegantDarkSurface,
    onPrimary = ElegantLavenderTextCombined,
    onSecondary = ElegantLavenderTextCombined,
    onBackground = ElegantTextPrimary,
    onSurface = ElegantTextPrimary,
    outline = ElegantBorder,
    surfaceVariant = ElegantDarkInnerSurface,
    onSurfaceVariant = ElegantTextSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NeonIndigo,
    secondary = ElectricPurple,
    tertiary = GoldenSunset,
    background = Color(0xFFF9FAFB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for drawing focus workbench
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
