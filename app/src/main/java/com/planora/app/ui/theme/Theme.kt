package com.planora.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.planora.app.data.AppSettings

data class ThemePreset(
    val name: String,
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
    val lightBg: Long,
    val darkBg: Long,
    val forceDark: Boolean = false,
)

val ThemePresets = listOf(
    ThemePreset("Colorful", 0xFF7C4DFF, 0xFFFF4F7B, 0xFF1CB5E0, 0xFFF7F2FF, 0xFF15121F),
    ThemePreset("Pastell", 0xFF9C8CF0, 0xFFF8A5C2, 0xFF8FD6E1, 0xFFFDF6FB, 0xFF1C1A22),
    ThemePreset("Pink", 0xFFE91E63, 0xFFFF80AB, 0xFFFFB74D, 0xFFFFF0F5, 0xFF22121A),
    ThemePreset("Lila", 0xFF7B1FA2, 0xFFBA68C8, 0xFF5C6BC0, 0xFFF6EEFA, 0xFF1A1220),
    ThemePreset("Minimalistisch", 0xFF212121, 0xFF757575, 0xFF9E9E9E, 0xFFFAFAFA, 0xFF121212),
    ThemePreset("Dark", 0xFFBB86FC, 0xFF03DAC5, 0xFFFF7597, 0xFF0E0E12, 0xFF0E0E12, forceDark = true),
    ThemePreset("Nature", 0xFF2E7D32, 0xFF8D6E63, 0xFF00897B, 0xFFF3F7EF, 0xFF121A13),
    ThemePreset("Aesthetic", 0xFFC08497, 0xFFD4A5A5, 0xFF9A8C98, 0xFFF8EDEB, 0xFF201A1C),
    ThemePreset("Modern", 0xFF2962FF, 0xFF00BFA5, 0xFFFF6D00, 0xFFF4F6FB, 0xFF0F1320),
)

val FontFamilies = listOf(
    "Standard" to FontFamily.Default,
    "Serif" to FontFamily.Serif,
    "Monospace" to FontFamily.Monospace,
    "Handschrift" to FontFamily.Cursive,
    "Sans" to FontFamily.SansSerif,
)

fun fontFamilyOf(i: Int): FontFamily = FontFamilies.getOrNull(i)?.second ?: FontFamily.Default

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/** Abstände abhängig von der Einstellung „kompakt / normal / großzügig“. */
object Spacing {
    @Composable
    fun gap(): Dp = when (LocalAppSettings.current.spacing) {
        0 -> 6.dp
        2 -> 14.dp
        else -> 10.dp
    }

    @Composable
    fun screen(): Dp = when (LocalAppSettings.current.spacing) {
        0 -> 10.dp
        2 -> 20.dp
        else -> 16.dp
    }
}

fun Color.onColor(): Color = if (luminance() > 0.55f) Color(0xFF1F1B24) else Color.White

private fun scaled(t: TextStyle, k: Float, family: FontFamily) =
    t.copy(fontSize = t.fontSize * k, lineHeight = t.lineHeight * k, fontFamily = family)

@Composable
fun PlanoraTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val preset = ThemePresets.getOrElse(settings.themePreset) { ThemePresets[0] }
    val dark = preset.forceDark || when (settings.darkMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val primary = settings.accentColor?.let { Color(it) } ?: Color(preset.primary)
    val secondary = Color(preset.secondary)
    val tertiary = Color(preset.tertiary)
    val bg = settings.customBackground?.let { Color(it) } ?: Color(if (dark) preset.darkBg else preset.lightBg)
    val surface = settings.customSurface?.let { Color(it) } ?: if (dark) lerp(bg, Color.White, 0.06f) else lerp(bg, Color.White, 0.6f)
    val onBg = if (bg.luminance() > 0.5f) Color(0xFF1F1B24) else Color(0xFFF2EEF7)
    val primaryLight = if (dark) lerp(primary, Color.White, 0.25f) else primary

    val scheme = if (dark) {
        darkColorScheme(
            primary = primaryLight, onPrimary = primaryLight.onColor(),
            primaryContainer = lerp(primary, bg, 0.55f), onPrimaryContainer = Color.White,
            secondary = secondary, onSecondary = secondary.onColor(),
            secondaryContainer = lerp(secondary, bg, 0.6f), onSecondaryContainer = Color.White,
            tertiary = tertiary, onTertiary = tertiary.onColor(),
            tertiaryContainer = lerp(tertiary, bg, 0.6f),
            background = bg, onBackground = onBg,
            surface = surface, onSurface = onBg,
            surfaceVariant = lerp(surface, Color.White, 0.06f), onSurfaceVariant = onBg.copy(alpha = 0.75f),
            surfaceContainer = lerp(bg, Color.White, 0.08f),
            surfaceContainerHigh = lerp(bg, Color.White, 0.11f),
            surfaceContainerLow = lerp(bg, Color.White, 0.04f),
            surfaceContainerHighest = lerp(bg, Color.White, 0.14f),
            outline = onBg.copy(alpha = 0.35f),
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = primary.onColor(),
            primaryContainer = lerp(primary, Color.White, 0.8f), onPrimaryContainer = lerp(primary, Color.Black, 0.5f),
            secondary = secondary, onSecondary = secondary.onColor(),
            secondaryContainer = lerp(secondary, Color.White, 0.78f), onSecondaryContainer = lerp(secondary, Color.Black, 0.55f),
            tertiary = tertiary, onTertiary = tertiary.onColor(),
            tertiaryContainer = lerp(tertiary, Color.White, 0.78f),
            background = bg, onBackground = onBg,
            surface = surface, onSurface = onBg,
            surfaceVariant = lerp(bg, primary, 0.06f), onSurfaceVariant = onBg.copy(alpha = 0.72f),
            surfaceContainer = lerp(bg, Color.White, 0.5f),
            surfaceContainerHigh = lerp(bg, Color.White, 0.35f),
            surfaceContainerLow = lerp(bg, Color.White, 0.7f),
            surfaceContainerHighest = lerp(bg, primary, 0.08f),
            outline = onBg.copy(alpha = 0.3f),
        )
    }
    val base = Typography()
    val k = settings.fontScale
    val f = fontFamilyOf(settings.fontFamily)
    val typography = Typography(
        displayLarge = scaled(base.displayLarge, k, f), displayMedium = scaled(base.displayMedium, k, f),
        displaySmall = scaled(base.displaySmall, k, f), headlineLarge = scaled(base.headlineLarge, k, f),
        headlineMedium = scaled(base.headlineMedium, k, f), headlineSmall = scaled(base.headlineSmall, k, f),
        titleLarge = scaled(base.titleLarge, k, f), titleMedium = scaled(base.titleMedium, k, f),
        titleSmall = scaled(base.titleSmall, k, f), bodyLarge = scaled(base.bodyLarge, k, f),
        bodyMedium = scaled(base.bodyMedium, k, f), bodySmall = scaled(base.bodySmall, k, f),
        labelLarge = scaled(base.labelLarge, k, f), labelMedium = scaled(base.labelMedium, k, f),
        labelSmall = scaled(base.labelSmall, k, f),
    )
    val c = settings.cornerScale
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((6 * c).dp),
        small = RoundedCornerShape((10 * c).dp),
        medium = RoundedCornerShape((16 * c).dp),
        large = RoundedCornerShape((22 * c).dp),
        extraLarge = RoundedCornerShape((30 * c).dp),
    )
    CompositionLocalProvider(LocalAppSettings provides settings) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
    }
}

/** Symbole im gewählten Stil (gefüllt, Umriss, abgerundet). */
object AppIcons {
    @Composable
    private fun style() = LocalAppSettings.current.iconStyle

    @Composable
    fun calendar(): ImageVector = when (style()) { 1 -> Icons.Outlined.CalendarMonth; 2 -> Icons.Rounded.CalendarMonth; else -> Icons.Filled.CalendarMonth }

    @Composable
    fun countdown(): ImageVector = when (style()) { 1 -> Icons.Outlined.HourglassTop; 2 -> Icons.Rounded.HourglassTop; else -> Icons.Filled.HourglassTop }

    @Composable
    fun favorites(): ImageVector = when (style()) { 1 -> Icons.Outlined.StarOutline; 2 -> Icons.Rounded.Star; else -> Icons.Filled.Star }

    @Composable
    fun checklist(): ImageVector = when (style()) { 1 -> Icons.Outlined.Checklist; 2 -> Icons.Rounded.Checklist; else -> Icons.Filled.Checklist }

    @Suppress("DEPRECATION")
    @Composable
    fun notes(): ImageVector = when (style()) { 1 -> Icons.Outlined.StickyNote2; 2 -> Icons.Rounded.StickyNote2; else -> Icons.Filled.StickyNote2 }

    @Composable
    fun dashboard(): ImageVector = when (style()) { 1 -> Icons.Outlined.Dashboard; 2 -> Icons.Rounded.Dashboard; else -> Icons.Filled.Dashboard }

    @Composable
    fun search(): ImageVector = when (style()) { 1 -> Icons.Outlined.Search; 2 -> Icons.Rounded.Search; else -> Icons.Filled.Search }

    @Composable
    fun settings(): ImageVector = when (style()) { 1 -> Icons.Outlined.Settings; 2 -> Icons.Rounded.Settings; else -> Icons.Filled.Settings }
}
