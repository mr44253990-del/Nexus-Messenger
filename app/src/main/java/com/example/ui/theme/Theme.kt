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

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = NexusPinkDark,
    secondary = NexusPurpleDark,
    background = GlassDark,
    surface = GlassSurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextLight,
    onSurface = TextLight,
    surfaceVariant = Color(0xFF3A2036),
    onSurfaceVariant = Color(0xFFEBC8DD)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = NexusPink,
    secondary = NexusPurple,
    background = GlassLight,
    surface = GlassSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFFFD6EA),
    onSurfaceVariant = Color(0xFF7A5269)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
