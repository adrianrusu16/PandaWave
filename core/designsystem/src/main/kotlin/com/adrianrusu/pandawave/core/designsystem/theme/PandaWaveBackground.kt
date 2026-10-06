package com.adrianrusu.pandawave.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.adrianrusu.pandawave.core.designsystem.tokens.LocalPandaWaveDesignTokens

/** A theme-owned glow; themes without a glow retain their solid surface. */
@Composable
fun Modifier.pandaWaveBackground(emphasized: Boolean = false): Modifier {
    val colors = LocalPandaWaveDesignTokens.current.colors
    val surface = Color(colors.surface)
    val glow = Color(colors.backgroundGlow)
    return drawWithCache {
        val brush = Brush.radialGradient(
            colors = listOf(glow.copy(alpha = if (emphasized) 0.75f else 0.24f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.35f),
            radius = (maxOf(size.width, size.height) * 0.9f).coerceAtLeast(1f)
        )
        onDrawBehind {
            drawRect(surface)
            if (glow != surface) drawRect(brush)
        }
    }
}
