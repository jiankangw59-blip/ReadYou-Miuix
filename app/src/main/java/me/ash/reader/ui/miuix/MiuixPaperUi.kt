package me.ash.reader.ui.miuix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paper-like reading backdrop: a whisper of warm grey at the very top that melts into an even,
 * archival-ivory page. Deliberately flat and light so body text keeps maximum contrast — no muddy
 * yellow/green wash.
 */
@Composable
internal fun MiuixPaperBackground(modifier: Modifier = Modifier) {
    val dark = MiuixMorandiIsDark
    val top = if (dark) Color(0xFF1B1A16) else Color(0xFFEFECE4)
    val body = if (dark) Color(0xFF171612) else Color(0xFFFAF8F3)

    Box(modifier = modifier.fillMaxSize().background(body)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(colors = listOf(top, Color.Transparent), endY = size.height * 0.35f))
        }
    }
}

/**
 * Floating filter capsule used by both the feeds home and the article list.
 * Nearly opaque plate + shadow + hairline highlight so scrolling content never bleeds through.
 */
@Composable
internal fun MiuixFilterCapsule(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MiuixMorandiIsDark
    val plate = if (dark) Slate.raisedDark else Slate.raised
    Row(
        modifier =
            modifier
                .fillMaxWidth(0.92f)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(plate)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = if (dark) 0.14f else 1f),
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(6.dp)
                .pointerInput(tabs.size) {
                    // Drag anywhere on the capsule: the finger's X decides the target tab.
                    fun pick(x: Float) {
                        val idx = ((x / size.width) * tabs.size).toInt().coerceIn(0, tabs.size - 1)
                        if (idx != selectedTabIndex) onTabSelected(idx)
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { pick(it.x) },
                        onHorizontalDrag = { change, _ -> pick(change.position.x) },
                    )
                },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedTabIndex
            Box(
                modifier =
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (selected) {
                                if (dark) MiuixAccentSky.copy(alpha = 0.22f) else Color(0xFFDCE6F7)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color =
                        when {
                            selected && dark -> Color(0xFF9CC4F0)
                            selected -> Color(0xFF2B6CB0)
                            dark -> Color(0xFFB9B9B4)
                            else -> Color(0xFF3A3A3C)
                        },
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** Kept as a named wrapper so the flow page reads the same as before. */
@Composable
internal fun MiuixFlowBottomTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) = MiuixFilterCapsule(tabs = tabs, selectedTabIndex = selectedTabIndex, onTabSelected = onTabSelected)


/**
 * Horizontal-swipe filter switching for the content area. A short left/right swipe moves one tab;
 * the gesture is ignored until it clearly dominates so vertical scrolling still wins.
 */
internal fun Modifier.filterSwipe(onSwitch: (Int) -> Unit): Modifier =
    this.pointerInput(Unit) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                val threshold = size.width * 0.18f
                when {
                    total <= -threshold -> onSwitch(1)
                    total >= threshold -> onSwitch(-1)
                }
            },
        ) { change, dragAmount ->
            total += dragAmount
            change.consume()
        }
    }
