package me.ash.reader.ui.miuix

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Monet-style contextual seeding, inspired by Halcyon:
 *
 * - feeds overview  -> wallpaper colour ([rememberWallpaperSeed])
 * - feed / group    -> that feed's icon colour ([rememberImageSeed])
 * - article         -> that article's cover colour ([rememberImageSeed])
 *
 * Everything is cached (LRU) and computed off the main thread on a 16x16 downsample, so switching
 * screens never re-decodes a full-size image and scrolling never triggers extraction.
 */
/**
 * Two-level cache for extracted seed colours: an in-memory LRU in front of a small on-disk map, so
 * a cold start does not have to re-decode avatars / covers just to recover their accent colour.
 */
private object SeedColorCache {
    private val cache =
        object : LinkedHashMap<String, Color>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Color>?): Boolean =
                size > 80
        }

    private const val PREFS = "miuix_seed_colors"
    @Volatile private var prefs: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    prefs =
                        context.applicationContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                }
            }
        }
    }

    fun get(key: String): Color? {
        synchronized(cache) { cache[key] }?.let { return it }
        val stored = prefs?.getInt(key.hashCode().toString(), 0) ?: 0
        if (stored == 0) return null
        val color = Color(stored)
        synchronized(cache) { cache[key] = color }
        return color
    }

    fun put(key: String, color: Color) {
        synchronized(cache) { cache[key] = color }
        prefs?.edit()?.putInt(key.hashCode().toString(), color.value.toInt())?.apply()
    }
}

/**
 * Holds the contextual seed colour of the screen that is currently on top. The Miuix theme reads it
 * once (see `MiuixAppTheme`), so switching screens only swaps one colour instead of re-nesting a
 * theme per page.
 */
object MiuixSeedState {
    // Screens can overlap while navigating: the newest registration wins, and disposing an older
    // screen simply reveals the previous seed instead of wiping the colour to null.
    private val stack = mutableStateListOf<Pair<Any, Color?>>()

    var seed: Color? by mutableStateOf(null)
        private set

    fun push(token: Any, color: Color?) {
        stack.removeAll { it.first === token }
        stack.add(token to color)
        seed = color
    }

    fun remove(token: Any) {
        val existed = stack.any { it.first === token }
        if (!existed) return
        stack.removeAll { it.first === token }
        seed = stack.lastOrNull()?.second
    }
}

/** Registers [seed] as the current contextual seed for as long as this screen is composed. */
@Composable
fun ProvideMiuixSeed(seed: Color?) {
    val token = remember { Any() }
    DisposableEffect(seed) {
        MiuixSeedState.push(token, seed)
        onDispose { MiuixSeedState.remove(token) }
    }
}

/**
 * "Wallpaper" seed. Read You already derives its Material You palette from the system wallpaper,
 * so the primary colour *is* the Monet wallpaper colour — no permission, no bitmap decoding and no
 * version-specific WallpaperColors API.
 */
@Composable
fun rememberWallpaperSeed(): Color = MaterialTheme.colorScheme.primary
/**
 * Extracts (and caches) the dominant colour of an image URL. Returns the last known colour while a
 * new one is being computed, so the UI never flashes.
 */
@Composable
fun rememberImageSeed(url: String?): State<Color?> {
    val context = LocalContext.current
    SeedColorCache.init(context)
    val fallback = MaterialTheme.colorScheme.primary
    return produceState<Color?>(
        initialValue = url?.let { SeedColorCache.get(it) },
        key1 = url,
    ) {
        if (url.isNullOrBlank()) {
            value = null
            return@produceState
        }
        SeedColorCache.get(url)?.let {
            value = it
            return@produceState
        }
        val extracted = extractSeedColor(context, url)
        android.util.Log.i("MiuixSeed", "extract url=" + url + " -> " + extracted)
        if (extracted != null) {
            SeedColorCache.put(url, extracted)
            value = extracted
        } else if (value == null) {
            value = fallback
        }
    }
}

/** Decodes a small bitmap through Coil and reduces it to one representative colour. */
private suspend fun extractSeedColor(context: Context, url: String): Color? =
    withContext(Dispatchers.IO) {
        runCatching {
                val request =
                    ImageRequest.Builder(context)
                        .data(url)
                        .size(48, 48)
                        .allowHardware(false)
                        .build()
                val result = context.imageLoader.execute(request)
                val bitmap =
                    (result as? SuccessResult)?.drawable?.let { drawable ->
                        Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                            .also { canvas ->
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(android.graphics.Canvas(canvas))
                            }
                    }
                bitmap?.let { dominantColor(it) }
            }
            .getOrNull()
    }

/**
 * Weighted dominant colour: picks the most saturated representative pixel group instead of a plain
 * average (which tends to end up grey-brown). Runs on a 16x16 downsample.
 */
private fun dominantColor(source: Bitmap): Color? {
    val small = Bitmap.createScaledBitmap(source, 16, 16, true)
    var bestScore = 0f
    var bestArgb = 0
    var bestSat = 0f
    for (y in 0 until small.height) {
        for (x in 0 until small.width) {
            val argb = small.getPixel(x, y)
            val alpha = android.graphics.Color.alpha(argb)
            if (alpha < 128) continue
            val r = android.graphics.Color.red(argb)
            val g = android.graphics.Color.green(argb)
            val b = android.graphics.Color.blue(argb)
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val value = max / 255f
            if (value < 0.12f || value > 0.97f) continue
            val saturation = if (max == 0) 0f else (max - min) / max.toFloat()
            // Prefer colourful pixels, never pure grey.
            val score = saturation * 2f + value
            if (score > bestScore || (score == bestScore && saturation > bestSat)) {
                bestScore = score
                bestArgb = argb
                bestSat = saturation
            }
        }
    }
    if (bestScore == 0f) return null
    return Color(bestArgb).copy(alpha = 1f)
}
