package me.ash.reader.ui.miuix

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * Hosts the Miuix (HyperOS / MIUI) design system with Halcyon-style contextual Monet seeding.
 *
 * The seed colour comes from [MiuixSeedState] (wallpaper on the feeds overview, the feed icon when
 * drilling into a feed, the article cover while reading) and falls back to the Material You primary
 * colour. The colour is animated, and only this composition level changes when the seed changes.
 */
@Composable
fun MiuixAppTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val fallback = MaterialTheme.colorScheme.primary
    val target = MiuixSeedState.seed ?: fallback
    val animatedSeed = target

    android.util.Log.i("MiuixSeed", "theme target=" + target + " state=" + MiuixSeedState.seed)
    val controller =
        remember(useDarkTheme, animatedSeed) {
            ThemeController(
                colorSchemeMode =
                    if (useDarkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light,
                keyColor = animatedSeed,
                paletteStyle = ThemePaletteStyle.Vibrant,
                isDark = useDarkTheme,
            )
        }
    MiuixTheme(controller = controller, content = content)
}
