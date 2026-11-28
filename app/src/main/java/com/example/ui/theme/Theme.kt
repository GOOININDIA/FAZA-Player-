package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.AppColorSource
import com.example.data.model.AppCustomColorPreset
import com.example.data.model.ThemeMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Centralized global theme configuration exposed across all FAZA Player screens & components.
 */
@Immutable
data class FazaThemeConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appColorSource: AppColorSource = AppColorSource.WALLPAPER,
    val customColorHex: Long = AppCustomColorPreset.ELECTRIC_VIOLET.seedHex,
    val isDark: Boolean = true,
    val isMonochrome: Boolean = false
)

/**
 * Exposes whether the active FAZA Player theme is currently rendering in AMOLED Dark mode.
 */
val LocalFazaIsDark = staticCompositionLocalOf { true }

/**
 * Exposes the active global [FazaThemeConfig] to every screen and component.
 */
val LocalFazaThemeConfig = staticCompositionLocalOf { FazaThemeConfig() }

/**
 * Signature Premium AMOLED Dark ColorScheme:
 * Pure obsidian AMOLED canvas, layered neutral surfaces,
 * Electric Purple / Vibrant Violet primary, and subtle Electric Blue highlights.
 */
val FazaAmoledDarkColorScheme: ColorScheme = darkColorScheme(
    primary = FazaElectricPurple,
    onPrimary = FazaOnElectricPurple,
    primaryContainer = FazaVioletContainerDark,
    onPrimaryContainer = FazaOnVioletContainerDark,
    secondary = FazaSecondaryDark,
    onSecondary = FazaOnSecondaryDark,
    secondaryContainer = FazaSecondaryContainerDark,
    onSecondaryContainer = FazaOnSecondaryContainerDark,
    tertiary = FazaElectricBlue,
    onTertiary = FazaOnElectricBlue,
    tertiaryContainer = FazaBlueContainerDark,
    onTertiaryContainer = FazaOnBlueContainerDark,
    background = FazaAmoledBlack,
    onBackground = FazaTextDarkPrimary,
    surface = FazaAmoledSurface,
    onSurface = FazaTextDarkPrimary,
    surfaceVariant = FazaAmoledSurfaceHigh,
    onSurfaceVariant = FazaTextDarkSecondary,
    surfaceContainerLowest = FazaAmoledSurfaceLowest,
    surfaceContainerLow = FazaAmoledSurfaceLow,
    surfaceContainer = FazaAmoledSurfaceContainer,
    surfaceContainerHigh = FazaAmoledSurfaceHigh,
    surfaceContainerHighest = FazaAmoledSurfaceHighest,
    outline = FazaAmoledOutline,
    outlineVariant = FazaAmoledOutlineVariant,
    error = FazaDarkError,
    onError = FazaOnDarkError
)

/**
 * Optional Crisp Light ColorScheme:
 * Luminous cool-pearl background, crisp pure-white cards,
 * Vibrant Violet primary, and subtle Electric Blue accents.
 */
val FazaCrispLightColorScheme: ColorScheme = lightColorScheme(
    primary = FazaLightPrimary,
    onPrimary = FazaLightOnPrimary,
    primaryContainer = FazaLightPrimaryContainer,
    onPrimaryContainer = FazaLightOnPrimaryContainer,
    secondary = FazaLightSecondary,
    onSecondary = FazaLightOnSecondary,
    secondaryContainer = FazaLightSecondaryContainer,
    onSecondaryContainer = FazaLightOnSecondaryContainer,
    tertiary = FazaLightTertiary,
    onTertiary = FazaLightOnTertiary,
    tertiaryContainer = FazaLightTertiaryContainer,
    onTertiaryContainer = FazaLightOnTertiaryContainer,
    background = FazaLightBg,
    onBackground = FazaTextLightPrimary,
    surface = FazaLightSurface,
    onSurface = FazaTextLightPrimary,
    surfaceVariant = FazaLightSurfaceHigh,
    onSurfaceVariant = FazaTextLightSecondary,
    surfaceContainerLowest = FazaLightSurfaceLowest,
    surfaceContainerLow = FazaLightSurfaceLow,
    surfaceContainer = FazaLightSurfaceContainer,
    surfaceContainerHigh = FazaLightSurfaceHigh,
    surfaceContainerHighest = FazaLightSurfaceHighest,
    outline = FazaLightOutline,
    outlineVariant = FazaLightOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White
)

fun rgbHexToHsl(colorHex: Long): Triple<Float, Float, Float> {
    val r = ((colorHex shr 16) and 0xFFL) / 255f
    val g = ((colorHex shr 8) and 0xFFL) / 255f
    val b = (colorHex and 0xFFL) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val l = (maxC + minC) / 2f
    if (maxC == minC) {
        return Triple(0f, 0f, l)
    }
    val d = maxC - minC
    val s = if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
    val h = when (maxC) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f))
        g -> ((b - r) / d + 2f)
        else -> ((r - g) / d + 4f)
    } * 60f
    return Triple(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

fun hslToRgbHex(hueDeg: Float, saturation: Float, lightness: Float): Long {
    val h = ((hueDeg % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

fun rgbHexToHsv(colorHex: Long): Triple<Float, Float, Float> {
    val r = ((colorHex shr 16) and 0xFFL) / 255f
    val g = ((colorHex shr 8) and 0xFFL) / 255f
    val b = (colorHex and 0xFFL) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val h = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val s = if (maxC == 0f) 0f else delta / maxC
    val v = maxC
    return Triple(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
}

fun hsvToRgbHex(hueDeg: Float, saturation: Float, value: Float): Long {
    val h = ((hueDeg % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

private fun hslComposeColor(hueDeg: Float, saturation: Float, lightness: Float): Color =
    Color(hslToRgbHex(hueDeg, saturation, lightness))

private fun relativeLuminance(color: Color): Float =
    0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

private fun bestContrastOnColor(bg: Color, darkInk: Color, lightInk: Color = Color.White): Color =
    if (relativeLuminance(bg) > 0.42f) darkInk else lightInk

fun formatColorHexLabel(colorHex: Long): String {
    val rgb = colorHex and 0xFFFFFFL
    return "#" + rgb.toString(16).uppercase().padStart(6, '0')
}

/**
 * Validates and parses user-entered HEX color strings such as "#6750A4", "6750A4", or "#FF6750A4".
 * Returns a normalized 0xFFRRGGBB Long, or null if the input is invalid.
 */
fun parseColorHexInput(raw: String): Long? {
    val cleaned = raw.trim().removePrefix("#").uppercase()
    val rgbHex = when (cleaned.length) {
        6 -> cleaned
        8 -> cleaned.substring(2)
        else -> return null
    }
    if (!rgbHex.all { it in '0'..'9' || it in 'A'..'F' }) return null
    val parsedRgb = rgbHex.toLongOrNull(16) ?: return null
    return 0xFF000000L or (parsedRgb and 0xFFFFFFL)
}

fun isMonochromeSeedColor(seedColorHex: Long): Boolean {
    if (seedColorHex == AppCustomColorPreset.MONOCHROME.seedHex) return true
    val (_, rawSat, _) = rgbHexToHsl(seedColorHex)
    return rawSat < 0.08f
}

private fun buildMonochromeColorScheme(isDark: Boolean): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = Color(0xFFE4E4E7),
            onPrimary = Color(0xFF09090B),
            primaryContainer = Color(0xFF27272A),
            onPrimaryContainer = Color(0xFFFAFAFA),
            secondary = Color(0xFFA1A1AA),
            onSecondary = Color(0xFF09090B),
            secondaryContainer = Color(0xFF1C1C20),
            onSecondaryContainer = Color(0xFFF4F4F5),
            tertiary = Color(0xFFD4D4D8),
            onTertiary = Color(0xFF09090B),
            tertiaryContainer = Color(0xFF27272A),
            onTertiaryContainer = Color(0xFFF4F4F5),
            background = FazaAmoledBlack,
            onBackground = FazaTextDarkPrimary,
            surface = FazaAmoledSurface,
            onSurface = FazaTextDarkPrimary,
            surfaceVariant = Color(0xFF1F1F23),
            onSurfaceVariant = FazaTextDarkSecondary,
            surfaceContainerLowest = FazaAmoledSurfaceLowest,
            surfaceContainerLow = Color(0xFF111113),
            surfaceContainer = Color(0xFF161619),
            surfaceContainerHigh = Color(0xFF1F1F23),
            surfaceContainerHighest = Color(0xFF2A2A2E),
            outline = Color(0xFF52525B),
            outlineVariant = Color(0xFF27272A),
            error = FazaDarkError,
            onError = FazaOnDarkError
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF18181B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE4E4E7),
            onPrimaryContainer = Color(0xFF09090B),
            secondary = Color(0xFF52525B),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF1F1F3),
            onSecondaryContainer = Color(0xFF18181B),
            tertiary = Color(0xFF3F3F46),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFE4E4E7),
            onTertiaryContainer = Color(0xFF18181B),
            background = FazaLightBg,
            onBackground = FazaTextLightPrimary,
            surface = FazaLightSurface,
            onSurface = FazaTextLightPrimary,
            surfaceVariant = Color(0xFFECECEF),
            onSurfaceVariant = FazaTextLightSecondary,
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFF4F4F5),
            surfaceContainerHigh = Color(0xFFECECEF),
            surfaceContainerHighest = Color(0xFFE4E4E7),
            outline = Color(0xFF71717A),
            outlineVariant = Color(0xFFE4E4E7),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }
}

fun buildCustomAppColorScheme(seedColorHex: Long, isDark: Boolean): ColorScheme {
    // True grayscale/monochrome theme when monochrome preset or near-zero saturation color is selected
    if (isMonochromeSeedColor(seedColorHex)) {
        return buildMonochromeColorScheme(isDark)
    }

    // Signature Purple (Electric Violet) uses our hand-tuned AMOLED obsidian & crisp light tokens
    if (seedColorHex == AppCustomColorPreset.ELECTRIC_VIOLET.seedHex) {
        return if (isDark) FazaAmoledDarkColorScheme else FazaCrispLightColorScheme
    }

    val (rawHue, rawSat, rawLight) = rgbHexToHsl(seedColorHex)
    val h = rawHue
    val s = rawSat.coerceIn(0.32f, 0.94f)
    // Harmonious secondary & tertiary hue shifts
    val tertiaryHue = (h + 320f) % 360f
    val secondaryHue = (h + 18f) % 360f

    return if (isDark) {
        val darkLightness = rawLight.coerceIn(0.64f, 0.78f)
        val primary = hslComposeColor(h, (s * 0.88f).coerceIn(0.45f, 0.95f), darkLightness)
        val darkInk = hslComposeColor(h, s, 0.10f)
        val primaryContainer = hslComposeColor(h, (s * 0.72f), 0.24f)
        val onPrimaryContainer = hslComposeColor(h, (s * 0.85f), 0.94f)
        val secondary = hslComposeColor(secondaryHue, (s * 0.45f), 0.76f)
        val secondaryContainer = hslComposeColor(secondaryHue, (s * 0.34f), 0.18f)
        val tertiary = hslComposeColor(tertiaryHue, (s * 0.75f).coerceIn(0.50f, 0.92f), 0.72f)
        val tertiaryContainer = hslComposeColor(tertiaryHue, (s * 0.62f), 0.20f)

        darkColorScheme(
            primary = primary,
            onPrimary = bestContrastOnColor(primary, darkInk, Color.White),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = bestContrastOnColor(secondary, darkInk, Color.White),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = hslComposeColor(secondaryHue, (s * 0.40f), 0.92f),
            tertiary = tertiary,
            onTertiary = bestContrastOnColor(tertiary, hslComposeColor(tertiaryHue, s, 0.10f), Color.White),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = hslComposeColor(tertiaryHue, (s * 0.80f), 0.92f),
            background = FazaAmoledBlack,
            onBackground = FazaTextDarkPrimary,
            surface = FazaAmoledSurface,
            onSurface = FazaTextDarkPrimary,
            surfaceVariant = hslComposeColor(h, (s * 0.08f), 0.12f),
            onSurfaceVariant = FazaTextDarkSecondary,
            surfaceContainerLowest = FazaAmoledSurfaceLowest,
            surfaceContainerLow = hslComposeColor(h, (s * 0.08f), 0.07f),
            surfaceContainer = hslComposeColor(h, (s * 0.09f), 0.095f),
            surfaceContainerHigh = hslComposeColor(h, (s * 0.10f), 0.13f),
            surfaceContainerHighest = hslComposeColor(h, (s * 0.10f), 0.17f),
            outline = hslComposeColor(h, (s * 0.08f), 0.30f),
            outlineVariant = hslComposeColor(h, (s * 0.08f), 0.17f),
            error = FazaDarkError,
            onError = FazaOnDarkError
        )
    } else {
        // Keep yellow/bright colors legible on light backgrounds by constraining primary lightness
        val lightLightness = if (h in 42f..72f) 0.40f else rawLight.coerceIn(0.36f, 0.48f)
        val primary = hslComposeColor(h, s, lightLightness)
        val darkInk = hslComposeColor(h, s, 0.12f)
        val primaryContainer = hslComposeColor(h, (s * 0.88f).coerceIn(0.35f, 0.95f), 0.92f)
        val onPrimaryContainer = hslComposeColor(h, s, 0.16f)
        val secondary = hslComposeColor(secondaryHue, (s * 0.52f), 0.44f)
        val secondaryContainer = hslComposeColor(secondaryHue, (s * 0.36f), 0.93f)
        val tertiary = hslComposeColor(tertiaryHue, (s * 0.72f), 0.44f)
        val tertiaryContainer = hslComposeColor(tertiaryHue, (s * 0.75f), 0.91f)

        lightColorScheme(
            primary = primary,
            onPrimary = bestContrastOnColor(primary, darkInk, Color.White),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = bestContrastOnColor(secondary, darkInk, Color.White),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = hslComposeColor(secondaryHue, (s * 0.52f), 0.16f),
            tertiary = tertiary,
            onTertiary = bestContrastOnColor(tertiary, hslComposeColor(tertiaryHue, s, 0.12f), Color.White),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = hslComposeColor(tertiaryHue, s, 0.16f),
            background = FazaLightBg,
            onBackground = FazaTextLightPrimary,
            surface = FazaLightSurface,
            onSurface = FazaTextLightPrimary,
            surfaceVariant = hslComposeColor(h, (s * 0.06f), 0.93f),
            onSurfaceVariant = FazaTextLightSecondary,
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = hslComposeColor(h, (s * 0.06f), 0.96f),
            surfaceContainerHigh = hslComposeColor(h, (s * 0.07f), 0.93f),
            surfaceContainerHighest = hslComposeColor(h, (s * 0.07f), 0.89f),
            outline = hslComposeColor(h, (s * 0.08f), 0.50f),
            outlineVariant = hslComposeColor(h, (s * 0.08f), 0.88f),
            error = Color(0xFFDC2626),
            onError = Color.White
        )
    }
}

val FazaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object PermissionScreenTokens {
    val RoyalBluePrimary = Color(0xFF7C3AED)
    val RoyalBlueHover = Color(0xFF6D28D9)
    val RoyalBluePressed = Color(0xFF5B21B6)
    val RoyalBlueAccentLight = Color(0xFF7C3AED)
    val RoyalBlueAccentDark = Color(0xFFA78BFA)
    val SparkleGold = Color(0xFF38BDF8)
    val SparkleYellow = Color(0xFFC4B5FD)
    val FolderLightBlue = Color(0xFF60A5FA)
    val FolderDeepBlue = Color(0xFF4C1D95)
}

@Composable
fun FazaPlayerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    appColorSource: AppColorSource = AppColorSource.WALLPAPER,
    customColorHex: Long = AppCustomColorPreset.ELECTRIC_VIOLET.seedHex,
    forceDarkStatusBar: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val useWallpaperColor = appColorSource == AppColorSource.WALLPAPER && dynamicColor
    val context = LocalContext.current
    val colorScheme = remember(isDark, useWallpaperColor, customColorHex, context) {
        when {
            !useWallpaperColor -> buildCustomAppColorScheme(customColorHex, isDark)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                runCatching {
                    if (isDark) {
                        dynamicDarkColorScheme(context)
                    } else {
                        dynamicLightColorScheme(context)
                    }
                }.getOrElse {
                    if (isDark) FazaAmoledDarkColorScheme else FazaCrispLightColorScheme
                }
            }
            isDark -> FazaAmoledDarkColorScheme
            else -> FazaCrispLightColorScheme
        }
    }

    val themeConfig = remember(themeMode, appColorSource, customColorHex, isDark, useWallpaperColor) {
        FazaThemeConfig(
            themeMode = themeMode,
            appColorSource = appColorSource,
            customColorHex = customColorHex,
            isDark = isDark,
            isMonochrome = !useWallpaperColor && isMonochromeSeedColor(customColorHex)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark && !forceDarkStatusBar
                    isAppearanceLightNavigationBars = !isDark && !forceDarkStatusBar
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalFazaIsDark provides isDark,
        LocalFazaThemeConfig provides themeConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FazaTypography,
            shapes = FazaShapes
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onBackground,
                content = content
            )
        }
    }
}
