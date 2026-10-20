package me.ash.reader.ui.miuix



import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.detectVerticalDragGestures

import androidx.compose.foundation.border

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.rounded.ArrowBack

import androidx.compose.material.icons.rounded.DoneAll

import androidx.compose.material.icons.rounded.OpenInNew

import androidx.compose.material.icons.rounded.Refresh

import androidx.compose.material.icons.rounded.Star

import androidx.compose.material.icons.rounded.StarBorder

import androidx.compose.material3.Icon

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import kotlinx.coroutines.launch

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.toSize



/**

 * ushio-md "cool slate" palette: one hue everywhere, hierarchy expressed only through small

 * lightness steps, a 1dp white inner highlight and a soft shadow. No pure-white cards.

 */

internal object Slate {

    val page = Color(0xFFECEFF4)

    val pageDark = Color(0xFF1A1D24)



    val raised = Color(0xFFE6EAF0)

    val raisedDark = Color(0xFF232832)



    val sunken = Color(0xFFDCE1EA)

    val sunkenDark = Color(0xFF20242D)



    val hairline = Color(0xFFB8C0CE)

    val grid = Color(0xFF8E97A8)



    val text = Color(0xFF1A202C)

    val textDark = Color(0xFFE6E9EF)

    val body = Color(0xFF222834)

    val bodyDark = Color(0xFFCED3DC)

    val muted = Color(0xFF8C96A8)

    val accent = Color(0xFF4C5678)

    val accentDark = Color(0xFF9AA6CE)

}



internal val slateDark: Boolean

    @Composable get() = MiuixMorandiIsDark



@Composable

internal fun slatePage(): Color = if (slateDark) Slate.pageDark else Slate.page



@Composable

internal fun slateRaised(): Color = if (slateDark) Slate.raisedDark else Slate.raised



@Composable

internal fun slateSunken(): Color = if (slateDark) Slate.sunkenDark else Slate.sunken



@Composable

internal fun slateText(): Color = if (slateDark) Slate.textDark else Slate.text



@Composable

internal fun slateBody(): Color = if (slateDark) Slate.bodyDark else Slate.body



@Composable

internal fun slateAccent(): Color = if (slateDark) Slate.accentDark else Slate.accent



/** Flat cool-slate page canvas. */

@Composable

internal fun MiuixSlateBackground(modifier: Modifier = Modifier) {

    Box(modifier = modifier.fillMaxSize().background(slatePage()))

}



/** Raised surface: 1dp white inner highlight + soft ambient shadow. */

@Composable

internal fun Modifier.slateRaisedSurface(

    shape: RoundedCornerShape = RoundedCornerShape(14.dp),

): Modifier =

    this

        .shadow(elevation = 3.dp, shape = shape, clip = false, ambientColor = Color(0xFF9AA5B8))

        .clip(shape)

        .background(slateRaised())

        .border(1.dp, Color.White.copy(alpha = if (slateDark) 0.10f else 0.65f), shape)



/** Squircle icon button in the raised material. */

@Composable

internal fun SlateIconButton(

    onClick: () -> Unit,

    modifier: Modifier = Modifier,

    content: @Composable () -> Unit,

) {

    Box(

        modifier =

            modifier.size(42.dp).slateRaisedSurface().clickableNoIndication(onClick),

        contentAlignment = Alignment.Center,

    ) {

        content()

    }

}



private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =

    this.clickable(onClick = onClick)



/**

 * Split floating capsule top bar: back square, two-line info capsule, action squares.

 */

@Composable

internal fun SlateReaderTopBar(

    source: String,

    meta: String,

    starred: Boolean,

    onBack: () -> Unit,

    onToggleStar: () -> Unit,

    onOpenBrowser: () -> Unit,

    modifier: Modifier = Modifier,

) {

    Row(

        modifier =

            modifier

                .fillMaxWidth()

                .statusBarsPadding()

                .padding(horizontal = 16.dp, vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically,

    ) {

        SlateIconButton(onClick = onBack) {

            Icon(

                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,

                contentDescription = "返回",

                tint = slateAccent(),

                modifier = Modifier.size(20.dp),

            )

        }



        Column(

            modifier =

                Modifier.weight(1f)

                    .padding(horizontal = 10.dp)

                    .height(46.dp)

                    .slateRaisedSurface()

                    .padding(horizontal = 12.dp),

            verticalArrangement = Arrangement.Center,

        ) {

            Text(

                text = source,

                color = if (slateDark) Color(0xFFB9C2DA) else Color(0xFF3B4461),

                fontSize = 14.sp,

                fontWeight = FontWeight.Bold,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis,

            )

            Text(

                text = meta,

                color = if (slateDark) Slate.muted else Color(0xFF98A1B3),

                fontSize = 11.sp,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis,

            )

        }



        SlateIconButton(onClick = onToggleStar) {

            Icon(

                imageVector = if (starred) Icons.Rounded.Star else Icons.Rounded.StarBorder,

                contentDescription = "收藏",

                tint = slateAccent(),

                modifier = Modifier.size(20.dp),

            )

        }

        Box(modifier = Modifier.width(8.dp))

        SlateIconButton(onClick = onOpenBrowser) {

            Icon(

                imageVector = Icons.Rounded.OpenInNew,

                contentDescription = "用浏览器打开",

                tint = slateAccent(),

                modifier = Modifier.size(20.dp),

            )

        }

    }

}





/** Floating list top bar: back square + title capsule + two action squares, all with white edges. */

@Composable

internal fun SlateListTopBar(

    title: String,

    onBack: () -> Unit,

    onDoneAll: () -> Unit,

    onRefresh: () -> Unit,

    modifier: Modifier = Modifier,

) {

    Row(

        modifier =

            modifier

                .fillMaxWidth()

                .statusBarsPadding()

                .padding(horizontal = 16.dp, vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically,

    ) {

        SlateIconButton(onClick = onBack) {

            Icon(

                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,

                contentDescription = "返回",

                tint = slateAccent(),

                modifier = Modifier.size(20.dp),

            )

        }

        Box(

            modifier =

                Modifier.weight(1f)

                    .padding(horizontal = 10.dp)

                    .height(46.dp)

                    .slateRaisedSurface()

                    .padding(horizontal = 14.dp),

            contentAlignment = Alignment.CenterStart,

        ) {

            Text(

                text = title,

                color = slateText(),

                fontSize = 15.sp,

                fontWeight = FontWeight.Bold,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis,

            )

        }

        SlateIconButton(onClick = onDoneAll) {

            Icon(

                imageVector = Icons.Rounded.DoneAll,

                contentDescription = "全部标记已读",

                tint = slateAccent(),

                modifier = Modifier.size(20.dp),

            )

        }

        Box(modifier = Modifier.width(8.dp))

        SlateIconButton(onClick = onRefresh) {

            Icon(

                imageVector = Icons.Rounded.Refresh,

                contentDescription = "刷新",

                tint = slateAccent(),

                modifier = Modifier.size(20.dp),

            )

        }

    }

}



/**

 * Zero-overhead scroll indicator.

 *

 * - Nothing is composed per frame: the thumb is drawn inside `Canvas` and the list state is read in

 *   the draw phase, so scrolling only invalidates this small draw node.

 * - While idle nothing is painted at all (`drawOnlyWhileActive`), so the component costs a single

 *   empty draw.

 * - Dragging maps finger Y to an item index and only issues `scrollToItem` when the index changes,

 *   keeping the touch loop allocation-free.

 */

@Composable

internal fun SlateMinimalScrollbar(

    listState: androidx.compose.foundation.lazy.LazyListState,

    modifier: Modifier = Modifier,

) {

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val touching = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }



    Box(

        modifier =

            modifier

                .width(18.dp)

                .fillMaxHeight()

                .pointerInput(Unit) {

                    var lastTarget = -1

                    detectVerticalDragGestures(

                        onDragStart = { touching.value = true },

                        onDragEnd = { touching.value = false },

                        onDragCancel = { touching.value = false },

                    ) { change, _ ->

                        change.consume()

                        val info = listState.layoutInfo

                        val total = info.totalItemsCount

                        if (total > 1) {

                            val ratio = (change.position.y / size.height).coerceIn(0f, 1f)

                            val target = (ratio * (total - 1)).toInt().coerceIn(0, total - 1)

                            if (target != lastTarget) {

                                lastTarget = target

                                scope.launch { listState.scrollToItem(target) }

                            }

                        }

                    }

                }

    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            val active = touching.value || listState.isScrollInProgress

            if (!active) return@Canvas



            val info = listState.layoutInfo

            val total = info.totalItemsCount

            val visible = info.visibleItemsInfo.size

            if (total <= visible || total == 0) return@Canvas



            val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0

            val scrollable = (total - visible).coerceAtLeast(1)

            val progress = (first.toFloat() / scrollable).coerceIn(0f, 1f)

            val thumbFraction = (visible.toFloat() / total).coerceIn(0.08f, 1f)



            val barWidth = 2.5.dp.toPx()

            val centerX = size.width - barWidth

            val thumbHeight = size.height * thumbFraction

            val thumbTop = (size.height - thumbHeight) * progress



            drawRoundRect(

                color = Color.Black.copy(alpha = 0.10f),

                topLeft = Offset(centerX, thumbTop),

                size = androidx.compose.ui.geometry.Size(barWidth, thumbHeight),

                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),

            )

        }

    }

}


