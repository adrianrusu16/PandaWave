package com.adrianrusu.pandawave.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.adrianrusu.pandawave.core.designsystem.tokens.LocalPandaWaveDesignTokens
import com.adrianrusu.pandawave.core.designsystem.tokens.PandaWaveColorTokens
import com.adrianrusu.pandawave.core.designsystem.tokens.ResourceDesignTokenProvider
import com.adrianrusu.pandawave.core.designsystem.tokens.mediumCorner
import com.adrianrusu.pandawave.core.designsystem.tokens.smallCorner
import com.adrianrusu.pandawave.core.model.theme.PandaWaveThemePreference

enum class PandaWaveThemeId(val displayName: String, val isDark: Boolean) {
    BambooGroveLight(
        displayName = "Bamboo Grove Light",
        isDark = false
    ),
    MoonlitBambooDark(
        displayName = "Moonlit Bamboo Dark",
        isDark = true
    ),
    ForestTechLight(
        displayName = "Forest Tech Light",
        isDark = false
    ),
    ForestTechDark(
        displayName = "Forest Tech Dark",
        isDark = true
    ),
    PandaWavePink(
        displayName = "PandaWave Pink",
        isDark = true
    )
}

data class PandaWaveThemeProfile(val id: PandaWaveThemeId) {
    val isDark: Boolean = id.isDark
}

val LocalPandaWaveThemeProfile = staticCompositionLocalOf {
    PandaWaveThemeProfile(id = PandaWaveThemeId.BambooGroveLight)
}

@Composable
fun PandaWaveTheme(
    darkTheme: Boolean,
    themePreference: PandaWaveThemePreference = PandaWaveThemePreference.SystemDefault,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeProfile = remember(themePreference, darkTheme) {
        themePreference.toThemeProfile(systemDark = darkTheme)
    }
    val tokens = remember(context, themeProfile.id) {
        ResourceDesignTokenProvider(context).load(themeProfile.id)
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(tokens.shape.smallCorner),
        small = RoundedCornerShape(tokens.shape.smallCorner),
        medium = RoundedCornerShape(tokens.shape.mediumCorner),
        large = RoundedCornerShape(tokens.shape.mediumCorner),
        extraLarge = RoundedCornerShape(tokens.shape.mediumCorner)
    )
    val typography = Typography(
        displayMedium = tokens.typography.display,
        headlineMedium = tokens.typography.display,
        titleLarge = tokens.typography.sectionTitle,
        titleMedium = tokens.typography.sectionTitle,
        titleSmall = tokens.typography.controlLabel,
        bodyLarge = tokens.typography.body,
        bodyMedium = tokens.typography.body,
        bodySmall = tokens.typography.metadata,
        labelLarge = tokens.typography.controlLabel,
        labelMedium = tokens.typography.controlLabel,
        labelSmall = tokens.typography.metadata
    )

    CompositionLocalProvider(
        LocalPandaWaveDesignTokens provides tokens,
        LocalPandaWaveThemeProfile provides themeProfile
    ) {
        MaterialTheme(
            colorScheme = tokens.colors.toColorScheme(themeProfile.isDark).let { scheme ->
                if (themeProfile.id == PandaWaveThemeId.PandaWavePink) {
                    scheme.withPinkSurfaces(tokens.colors)
                } else {
                    scheme
                }
            },
            shapes = shapes,
            typography = typography,
            content = content
        )
    }
}

fun PandaWaveThemePreference.toThemeProfile(systemDark: Boolean): PandaWaveThemeProfile = when (this) {
    PandaWaveThemePreference.SystemDefault ->
        if (systemDark) {
            PandaWaveThemeProfile(id = PandaWaveThemeId.MoonlitBambooDark)
        } else {
            PandaWaveThemeProfile(id = PandaWaveThemeId.BambooGroveLight)
        }

    PandaWaveThemePreference.BambooGroveLight ->
        PandaWaveThemeProfile(id = PandaWaveThemeId.BambooGroveLight)

    PandaWaveThemePreference.MoonlitBambooDark ->
        PandaWaveThemeProfile(id = PandaWaveThemeId.MoonlitBambooDark)

    PandaWaveThemePreference.ForestTechLight ->
        PandaWaveThemeProfile(id = PandaWaveThemeId.ForestTechLight)

    PandaWaveThemePreference.ForestTechDark ->
        PandaWaveThemeProfile(id = PandaWaveThemeId.ForestTechDark)

    PandaWaveThemePreference.PandaWavePink ->
        PandaWaveThemeProfile(id = PandaWaveThemeId.PandaWavePink)
}

private fun ColorScheme.withPinkSurfaces(colors: PandaWaveColorTokens): ColorScheme = copy(
    primaryContainer = Color(colors.backgroundGlow),
    onPrimaryContainer = Color(colors.onSurface),
    secondaryContainer = Color(colors.surfaceVariant),
    onSecondaryContainer = Color(colors.secondary),
    tertiary = Color(colors.secondary),
    onTertiary = Color(colors.onSecondary),
    tertiaryContainer = Color(colors.backgroundGlow),
    onTertiaryContainer = Color(colors.onSurface),
    background = Color(colors.surface),
    onBackground = Color(colors.onSurface),
    surfaceDim = Color(colors.surface),
    surfaceBright = Color(colors.surfaceVariant),
    surfaceContainerLowest = Color(colors.surface),
    surfaceContainerLow = Color(colors.surface),
    surfaceContainer = Color(colors.surfaceVariant),
    surfaceContainerHigh = Color(colors.surfaceVariant),
    surfaceContainerHighest = Color(colors.surfaceVariant),
    surfaceTint = Color(colors.primary),
    inverseSurface = Color(colors.secondary),
    inverseOnSurface = Color(colors.onSecondary),
    inversePrimary = Color(colors.backgroundGlow),
    outline = Color(colors.onSurfaceVariant).copy(alpha = colors.outlineAlpha),
    outlineVariant = Color(colors.onSurfaceVariant).copy(alpha = colors.outlineVariantAlpha)
)

private fun PandaWaveColorTokens.toColorScheme(darkTheme: Boolean): ColorScheme {
    val primary = Color(this.primary)
    val onPrimary = Color(this.onPrimary)
    val secondary = Color(this.secondary)
    val onSecondary = Color(this.onSecondary)
    val surface = Color(this.surface)
    val onSurface = Color(this.onSurface)
    val surfaceVariant = Color(this.surfaceVariant)
    val onSurfaceVariant = Color(this.onSurfaceVariant)
    val error = Color(this.error)
    val onError = Color(this.onError)

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            error = error,
            onError = onError
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            error = error,
            onError = onError
        )
    }
}
