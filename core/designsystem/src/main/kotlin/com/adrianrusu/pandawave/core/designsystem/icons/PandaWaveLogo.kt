package com.adrianrusu.pandawave.core.designsystem.icons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.adrianrusu.pandawave.core.designsystem.R

/** Preserve fine logo edges when the full-resolution mark is drawn at small icon sizes. */
@Composable
fun pandaWaveLogoPainter(size: Dp): Painter {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val pixels = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val bitmapPainter = remember(context, configuration, pixels) {
        val drawable = context.resources.getDrawable(R.drawable.pandawave_ic_logo, context.theme)
        (drawable as? BitmapDrawable)?.let { bitmapDrawable ->
            // Use the original pixels; resource density scaling would discard detail before
            // the logo is reduced to its final size below.
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.pandawave_ic_logo,
                BitmapFactory.Options().apply { inScaled = false }
            ) ?: bitmapDrawable.bitmap
            // Reduce in stages so thin outlines are averaged instead of skipped by a large
            // one-step GPU reduction. Remember the result at the actual display density.
            var scaled = bitmap
            while (scaled.width / 2 >= pixels) {
                scaled = Bitmap.createScaledBitmap(scaled, scaled.width / 2, scaled.height / 2, true)
            }
            if (scaled.width > pixels) {
                scaled = Bitmap.createScaledBitmap(scaled, pixels, pixels, true)
            }
            BitmapPainter(scaled.asImageBitmap(), filterQuality = FilterQuality.Low)
        }
    }
    // Resource overlays may replace the base PNG with a vector logo.
    return bitmapPainter ?: painterResource(R.drawable.pandawave_ic_logo)
}
