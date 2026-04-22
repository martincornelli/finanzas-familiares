package com.finanzasfamiliares.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BrandGreen = Color(0xFF176B5A)
val BrandGreenDark = Color(0xFF0E4F43)
val BrandGreenContainer = Color(0xFFD9EFE8)
val BrandAmber = Color(0xFFFFB547)
val BrandAmberContainer = Color(0xFFFFE7B8)
val BrandBlue = Color(0xFF2563EB)
val BrandBlueContainer = Color(0xFFDCE7FF)
val BrandInk = Color(0xFF1E2528)
val BrandMuted = Color(0xFF63706D)
val BrandCanvas = Color(0xFFF5F7F2)
val BrandSurface = Color(0xFFFFFFFF)
val BrandSurfaceLow = Color(0xFFE9F1EC)
val BrandOutline = Color(0xFFC3D1CA)

val Green700  = Color(0xFF237A50)
val Green100  = Color(0xFFDDF3E5)
val Yellow700 = Color(0xFFB26A00)
val Yellow100 = Color(0xFFFFE7B8)
val Red700    = Color(0xFFB42318)
val Red100    = Color(0xFFFFDAD6)

data class MarginColors(val background: Color, val content: Color)

fun marginColors(amount: Double, greenThreshold: Double = 20000.0, yellowThreshold: Double = 5000.0): MarginColors = when {
    amount >= greenThreshold  -> MarginColors(Green100, Green700)
    amount >= yellowThreshold -> MarginColors(Yellow100, Yellow700)
    else                      -> MarginColors(Red100, Red700)
}

private data class AccentPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color
)

private fun accentPalette(accentColor: AppAccentColor, darkTheme: Boolean): AccentPalette =
    when (accentColor) {
        AppAccentColor.GREEN -> if (darkTheme) {
            AccentPalette(Color(0xFF7ED7C2), Color(0xFF00382E), Color(0xFF0E4F43), Color(0xFFD9EFE8))
        } else {
            AccentPalette(BrandGreen, Color.White, BrandGreenContainer, BrandGreenDark)
        }
        AppAccentColor.BLUE -> if (darkTheme) {
            AccentPalette(Color(0xFFAFC6FF), Color(0xFF002E69), Color(0xFF153F8A), Color(0xFFDCE7FF))
        } else {
            AccentPalette(Color(0xFF2563EB), Color.White, Color(0xFFDCE7FF), Color(0xFF102E72))
        }
        AppAccentColor.TEAL -> if (darkTheme) {
            AccentPalette(Color(0xFF5EEAD4), Color(0xFF003731), Color(0xFF115E59), Color(0xFFCCFBF1))
        } else {
            AccentPalette(Color(0xFF0F766E), Color.White, Color(0xFFCCFBF1), Color(0xFF134E4A))
        }
        AppAccentColor.INDIGO -> if (darkTheme) {
            AccentPalette(Color(0xFFC7D2FE), Color(0xFF1E1B4B), Color(0xFF3730A3), Color(0xFFE0E7FF))
        } else {
            AccentPalette(Color(0xFF4F46E5), Color.White, Color(0xFFE0E7FF), Color(0xFF312E81))
        }
        AppAccentColor.VIOLET -> if (darkTheme) {
            AccentPalette(Color(0xFFDDD6FE), Color(0xFF2E1065), Color(0xFF5B21B6), Color(0xFFEDE9FE))
        } else {
            AccentPalette(Color(0xFF7C3AED), Color.White, Color(0xFFEDE9FE), Color(0xFF4C1D95))
        }
        AppAccentColor.SLATE -> if (darkTheme) {
            AccentPalette(Color(0xFFCBD5E1), Color(0xFF0F172A), Color(0xFF334155), Color(0xFFE2E8F0))
        } else {
            AccentPalette(Color(0xFF475569), Color.White, Color(0xFFE2E8F0), Color(0xFF1E293B))
        }
    }

private fun lightScheme(accentColor: AppAccentColor): ColorScheme {
    val accent = accentPalette(accentColor, darkTheme = false)
    val brandCanvas = lerp(Color.White, accent.primaryContainer, 0.24f)
    val brandSurfaceLow = lerp(Color.White, accent.primaryContainer, 0.58f)
    val brandSurface = lerp(Color.White, accent.primaryContainer, 0.70f)
    val brandSurfaceHigh = lerp(Color.White, accent.primaryContainer, 0.80f)
    val brandOutline = lerp(Color(0xFFCBD5D0), accent.primary, 0.22f)
    val brandOutlineVariant = lerp(brandSurface, accent.primary, 0.18f)
    val brandMuted = lerp(Color(0xFF5E6864), accent.onPrimaryContainer, 0.10f)
    return lightColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = BrandAmber,
        onSecondary = Color(0xFF3B2600),
        secondaryContainer = BrandAmberContainer,
        onSecondaryContainer = Color(0xFF392600),
        tertiary = BrandBlue,
        onTertiary = Color.White,
        tertiaryContainer = BrandBlueContainer,
        onTertiaryContainer = Color(0xFF102E72),
        error = Red700,
        onError = Color.White,
        errorContainer = Red100,
        onErrorContainer = Color(0xFF410002),
        background = brandCanvas,
        onBackground = BrandInk,
        surface = BrandSurface,
        onSurface = BrandInk,
        surfaceVariant = brandSurfaceLow,
        onSurfaceVariant = brandMuted,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = brandSurfaceLow,
        surfaceContainer = brandSurface,
        surfaceContainerHigh = brandSurfaceHigh,
        outline = brandOutline,
        outlineVariant = brandOutlineVariant
    )
}

private fun darkScheme(accentColor: AppAccentColor): ColorScheme {
    val accent = accentPalette(accentColor, darkTheme = true)
    val darkCanvas = lerp(Color(0xFF101412), accent.primaryContainer, 0.14f)
    val darkSurface = lerp(Color(0xFF121715), accent.primaryContainer, 0.12f)
    val darkSurfaceLow = lerp(Color(0xFF171B19), accent.primaryContainer, 0.20f)
    val darkSurfaceContainer = lerp(Color(0xFF1D2421), accent.primaryContainer, 0.24f)
    val darkSurfaceHigh = lerp(Color(0xFF26302C), accent.primaryContainer, 0.24f)
    val darkOutline = lerp(Color(0xFF85918C), accent.primary, 0.12f)
    val darkOutlineVariant = lerp(Color(0xFF3F4945), accent.primary, 0.16f)
    return darkColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = Color(0xFFFFC875),
        onSecondary = Color(0xFF3B2600),
        secondaryContainer = Color(0xFF5A3A00),
        onSecondaryContainer = Color(0xFFFFE7B8),
        tertiary = Color(0xFFAFC6FF),
        onTertiary = Color(0xFF002E69),
        tertiaryContainer = Color(0xFF153F8A),
        onTertiaryContainer = Color(0xFFDCE7FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = darkCanvas,
        onBackground = Color(0xFFEAF1EE),
        surface = darkSurface,
        onSurface = Color(0xFFEAF1EE),
        surfaceVariant = darkSurfaceContainer,
        onSurfaceVariant = Color(0xFFC3D1CA),
        surfaceContainerLowest = Color(0xFF0B110F),
        surfaceContainerLow = darkSurfaceLow,
        surfaceContainer = darkSurfaceContainer,
        surfaceContainerHigh = darkSurfaceHigh,
        outline = darkOutline,
        outlineVariant = darkOutlineVariant
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun FinanzasTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentColor: AppAccentColor = AppAccentColor.GREEN,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme(accentColor) else lightScheme(accentColor),
        typography  = Typography,
        shapes = AppShapes,
        content     = content
    )
}
