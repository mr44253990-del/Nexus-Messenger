package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme(val label: String, val key: String) {
    PINK_GLASS("Pink Glass", "pink_glass"),
    OCEAN("Ocean Blue", "ocean"),
    SUNSET("Sunset Orange", "sunset"),
    FOREST("Forest Green", "forest"),
    LAVENDER("Lavender Purple", "lavender"),
    MIDNIGHT("Midnight Blue", "midnight"),
    ROSE_GOLD("Rose Gold", "rose_gold"),
    SYSTEM("System Default", "system")
}

fun getThemeColors(theme: AppTheme, darkTheme: Boolean): ThemeColors {
    if (theme == AppTheme.SYSTEM) {
        return if (darkTheme) getDarkColors(AppTheme.PINK_GLASS) else getLightColors(AppTheme.PINK_GLASS)
    }
    return if (darkTheme) getDarkColors(theme) else getLightColors(theme)
}

data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val accent: Color,
    val glass: Color,
    val glassLight: Color,
    val glassSurface: Color,
    val glassBorder: Color
)

fun getLightColors(theme: AppTheme): ThemeColors {
    val (primary, primaryDark, accent, glass) = when (theme) {
        AppTheme.PINK_GLASS -> ThemeColors(PinkPrimary, PinkPrimaryDark, PinkAccent, PinkGlass, PinkPrimary, Color.White, PinkPrimaryLight, Color(0xFF880E4F), PinkGlassLight, PinkSurface, GlassWhite, GlassBorder, Color(0xFFB00020), PinkAccent, PinkGlass, PinkGlassLight, PinkSurface, GlassBorder)
        AppTheme.OCEAN -> ThemeColors(OceanPrimary, OceanPrimaryDark, OceanAccent, OceanGlass, OceanPrimary, Color.White, Color(0xFFB2EBF2), Color(0xFF004D40), OceanGlass.copy(alpha = 0.15f), OceanGlass.copy(alpha = 0.08f), GlassWhite, GlassBorder, Color(0xFFB00020), OceanAccent, OceanGlass, OceanGlass.copy(alpha = 0.15f), OceanGlass.copy(alpha = 0.08f), GlassBorder)
        AppTheme.SUNSET -> ThemeColors(SunsetPrimary, SunsetPrimaryDark, SunsetAccent, SunsetGlass, SunsetPrimary, Color.White, Color(0xFFFFCCBC), Color(0xFFBF360C), SunsetGlass.copy(alpha = 0.15f), SunsetGlass.copy(alpha = 0.08f), GlassWhite, GlassBorder, Color(0xFFB00020), SunsetAccent, SunsetGlass, SunsetGlass.copy(alpha = 0.15f), SunsetGlass.copy(alpha = 0.08f), GlassBorder)
        AppTheme.FOREST -> ThemeColors(ForestPrimary, ForestPrimaryDark, ForestAccent, ForestGlass, ForestPrimary, Color.White, Color(0xFFC8E6C9), Color(0xFF1B5E20), ForestGlass.copy(alpha = 0.15f), ForestGlass.copy(alpha = 0.08f), GlassWhite, GlassBorder, Color(0xFFB00020), ForestAccent, ForestGlass, ForestGlass.copy(alpha = 0.15f), ForestGlass.copy(alpha = 0.08f), GlassBorder)
        AppTheme.LAVENDER -> ThemeColors(LavenderPrimary, LavenderPrimaryDark, LavenderAccent, LavenderGlass, LavenderPrimary, Color.White, Color(0xFFE1BEE7), Color(0xFF4A148C), LavenderGlass.copy(alpha = 0.15f), LavenderGlass.copy(alpha = 0.08f), GlassWhite, GlassBorder, Color(0xFFB00020), LavenderAccent, LavenderGlass, LavenderGlass.copy(alpha = 0.15f), LavenderGlass.copy(alpha = 0.08f), GlassBorder)
        AppTheme.MIDNIGHT -> ThemeColors(MidnightPrimary, MidnightPrimaryDark, MidnightAccent, MidnightGlass, MidnightPrimary, Color.White, Color(0xFFC5CAE9), Color(0xFF1A237E), MidnightGlass.copy(alpha = 0.15f), MidnightGlass.copy(alpha = 0.08f), GlassWhite, GlassBorder, Color(0xFFB00020), MidnightAccent, MidnightGlass, MidnightGlass.copy(alpha = 0.15f), MidnightGlass.copy(alpha = 0.08f), GlassBorder)
        AppTheme.ROSE_GOLD -> ThemeColors(RoseGoldPrimary, RoseGoldPrimaryDark, RoseGoldAccent, RoseGoldGlass, RoseGoldPrimary, Color.White, Color(0xFFFFE0B2), Color(0xFF795548), RoseGoldGlass.copy(alpha = 0.15f), RoseGoldGlass.copy(alpha = 0.08f), GlassWhite, GlassBorder, Color(0xFFB00020), RoseGoldAccent, RoseGoldGlass, RoseGoldGlass.copy(alpha = 0.15f), RoseGoldGlass.copy(alpha = 0.08f), GlassBorder)
        AppTheme.SYSTEM -> getLightColors(AppTheme.PINK_GLASS)
    }
    return ThemeColors(
        primary = primary, onPrimary = Color.White,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = Color.White,
        background = Color(0xFFFFF0F5), onBackground = Color(0xFF1C1C1E),
        surface = Color.White, onSurface = Color(0xFF1C1C1E),
        surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
        error = error, accent = accent,
        glass = glass, glassLight = glassLight,
        glassSurface = glassSurface, glassBorder = glassBorder
    )
}

fun getDarkColors(theme: AppTheme): ThemeColors {
    return when (theme) {
        AppTheme.PINK_GLASS -> ThemeColors(
            PinkPrimary, Color.White, Color(0x33E91E8C), PinkPrimaryLight,
            PinkAccent, Color.White,
            Color(0xFF0D0D0D), Color(0xFFF2F2F7),
            Color(0xFF1A1A1A), Color(0xFFF2F2F7),
            Color(0xFF2C2C2E), Color(0xFFAEB0B2),
            Color(0xFFCF6679), PinkAccent,
            Color(0x33E91E8C), Color(0x1AE91E8C), Color(0x0DE91E8C), Color(0x33E91E8C)
        )
        AppTheme.OCEAN -> ThemeColors(
            OceanPrimary, Color.White, Color(0x3300BCD4), Color(0xFFB2EBF2),
            OceanAccent, Color.White,
            Color(0xFF0A0D10), Color(0xFFF2F2F7),
            Color(0xFF1A1D20), Color(0xFFF2F2F7),
            Color(0xFF2C2E30), Color(0xFFAEB0B2),
            Color(0xFFCF6679), OceanAccent,
            Color(0x3300BCD4), Color(0x1A00BCD4), Color(0x0D00BCD4), Color(0x3300BCD4)
        )
        AppTheme.SUNSET -> ThemeColors(
            SunsetPrimary, Color.White, Color(0x33FF5722), Color(0xFFFFCCBC),
            SunsetAccent, Color.White,
            Color(0xFF0D0A0A), Color(0xFFF2F2F7),
            Color(0xFF1A1515), Color(0xFFF2F2F7),
            Color(0xFF2C2727), Color(0xFFAEB0B2),
            Color(0xFFCF6679), SunsetAccent,
            Color(0x33FF5722), Color(0x1AFF5722), Color(0x0DFF5722), Color(0x33FF5722)
        )
        AppTheme.FOREST -> ThemeColors(
            ForestPrimary, Color.White, Color(0x334CAF50), Color(0xFFC8E6C9),
            ForestAccent, Color.White,
            Color(0xFF0A0D0A), Color(0xFFF2F2F7),
            Color(0xFF151A15), Color(0xFFF2F2F7),
            Color(0xFF272C27), Color(0xFFAEB0B2),
            Color(0xFFCF6679), ForestAccent,
            Color(0x334CAF50), Color(0x1A4CAF50), Color(0x0D4CAF50), Color(0x334CAF50)
        )
        AppTheme.LAVENDER -> ThemeColors(
            LavenderPrimary, Color.White, Color(0x339C27B0), Color(0xFFE1BEE7),
            LavenderAccent, Color.White,
            Color(0xFF0D0A0D), Color(0xFFF2F2F7),
            Color(0xFF1A151A), Color(0xFFF2F2F7),
            Color(0xFF2C272C), Color(0xFFAEB0B2),
            Color(0xFFCF6679), LavenderAccent,
            Color(0x339C27B0), Color(0x1A9C27B0), Color(0x0D9C27B0), Color(0x339C27B0)
        )
        AppTheme.MIDNIGHT -> ThemeColors(
            MidnightPrimary, Color.White, Color(0x333F51B5), Color(0xFFC5CAE9),
            MidnightAccent, Color.White,
            Color(0xFF0A0A0D), Color(0xFFF2F2F7),
            Color(0xFF15151A), Color(0xFFF2F2F7),
            Color(0xFF27272C), Color(0xFFAEB0B2),
            Color(0xFFCF6679), MidnightAccent,
            Color(0x333F51B5), Color(0x1A3F51B5), Color(0x0D3F51B5), Color(0x333F51B5)
        )
        AppTheme.ROSE_GOLD -> ThemeColors(
            RoseGoldPrimary, Color.White, Color(0x33E8A87C), Color(0xFFFFE0B2),
            RoseGoldAccent, Color.White,
            Color(0xFF0D0B0A), Color(0xFFF2F2F7),
            Color(0xFF1A1615), Color(0xFFF2F2F7),
            Color(0xFF2C2625), Color(0xFFAEB0B2),
            Color(0xFFCF6679), RoseGoldAccent,
            Color(0x33E8A87C), Color(0x1AE8A87C), Color(0x0DE8A87C), Color(0x33E8A87C)
        )
        AppTheme.SYSTEM -> getDarkColors(AppTheme.PINK_GLASS)
    }
}

@Composable
fun EBChatTheme(
    theme: AppTheme = AppTheme.PINK_GLASS,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = getThemeColors(theme, darkTheme)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            error = colors.error
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            error = colors.error
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalThemeColors provides colors
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}

object LocalThemeColors {
    val current: ThemeColors
        @Composable get() = androidx.compose.runtime.compositionLocalOf { getLightColors(AppTheme.PINK_GLASS) }.current
}