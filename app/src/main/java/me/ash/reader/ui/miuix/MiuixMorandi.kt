package me.ash.reader.ui.miuix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/** Low-saturation Morandi accents used by the shortcut rings and tile icons. */
internal val MiuixAccentAmber = Color(0xFFD8A657)
internal val MiuixAccentSky = Color(0xFF5B8DB8)
internal val MiuixAccentCoral = Color(0xFFD98E73)
internal val MiuixAccentRose = Color(0xFFC98B92)

/** True when the current Miuix palette is dark; used by the feeds redesign. */
internal val MiuixMorandiIsDark: Boolean
    @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

internal val MiuixMorandiTextStrong: Color
    @Composable get() = if (MiuixMorandiIsDark) Color(0xFFF2EEE6) else Color(0xFF1C1C1E)

internal val MiuixMorandiTextSecondary: Color
    @Composable get() = if (MiuixMorandiIsDark) Color(0xFFB4B0A6) else Color(0xFF6E6E73)


/**
 * Full-bleed Morandi mesh gradient: several very large radial washes over a warm base.
 * Replaces the saturated blue/purple backdrop so the transparent top bar blends into the page.
 */
@Composable
internal fun MiuixMorandiBackground(modifier: Modifier = Modifier) {
    val dark = MiuixMorandiIsDark
    val base = if (dark) Color(0xFF171713) else Color(0xFFF6F3EC)

    val washA = if (dark) Color(0xFF2A342B) else Color(0xFFE5E8DC) // sage
    val washB = if (dark) Color(0xFF322925) else Color(0xFFF5E6E0) // blush
    val washC = if (dark) Color(0xFF33301F) else Color(0xFFEFEAD8) // cream

    Box(modifier = modifier.fillMaxSize().background(base)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = size.minDimension * 1.05f

            drawRect(
                brush =
                    Brush.radialGradient(
                        colors = listOf(washA.copy(alpha = if (dark) 0.55f else 0.9f), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.08f),
                        radius = r,
                    )
            )
            drawRect(
                brush =
                    Brush.radialGradient(
                        colors = listOf(washB.copy(alpha = if (dark) 0.5f else 0.85f), Color.Transparent),
                        center = Offset(w * 0.95f, h * 0.22f),
                        radius = r * 0.92f,
                    )
            )
            drawRect(
                brush =
                    Brush.radialGradient(
                        colors = listOf(washC.copy(alpha = if (dark) 0.45f else 0.8f), Color.Transparent),
                        center = Offset(w * 0.72f, h * 0.92f),
                        radius = r * 0.9f,
                    )
            )
        }
    }
}
