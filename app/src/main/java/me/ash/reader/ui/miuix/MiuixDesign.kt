package me.ash.reader.ui.miuix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Halcyon-inspired glassmorphism palette: a soft, colourful "liquid" backdrop (derived from the
 * current Miuix palette) with frosted translucent cards floating on top of it.
 */
@Composable
internal fun miuixCanvasColor(): Color {
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFF101014) else Color(0xFFF4F4F7)
}

/** Glass surface colour used by every card of the Miuix skin. */
@Composable
internal fun miuixGlassColor(): Color {
    val colors = MiuixTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    // Pre-blended, opaque glass colour: a translucent fill would blend over the whole card area on
    // every frame while scrolling.
    val base = colors.surface
    return if (isDark) androidx.compose.ui.graphics.lerp(base, Color(0xFF2A2A2E), 0.72f)
    else androidx.compose.ui.graphics.lerp(base, Color.White, 0.62f)
}

@Composable
internal fun miuixGlassCardColors() = CardDefaults.defaultColors(color = miuixGlassColor())

/**
 * The soft "liquid" backdrop. Four large radial blobs tinted with the current palette sit on the
 * flat canvas; the brushes are cached with drawWithCache so scrolling never re-creates them.
 */
@Composable
internal fun MiuixGlassBackground(modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    val targetSeed = MiuixSeedState.seed ?: colors.primary
    val isDark = colors.background.luminance() < 0.5f

    // Palette switches once; colour interpolation happens in the draw phase (no recomposition).
    val animatedSeed =
        androidx.compose.animation.animateColorAsState(
            targetValue = targetSeed,
            animationSpec =
                androidx.compose.animation.core.tween(
                    durationMillis = 700,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                ),
            label = "miuixSeed",
        )

    // One layer, two gradients, brushes cached per size+seed: far less overdraw than four
    // full-screen radial gradients redrawn every frame.
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawWithCache {
                    val sizeSnapshot = size
                    var cachedSeed: Color = Color.Unspecified
                    var topBrush: Brush? = null
                    var bottomBrush: Brush? = null
                    onDrawBehind {
                        val seed = animatedSeed.value
                        drawRect(colors.surface)
                        drawRect(seed.copy(alpha = if (isDark) 0.22f else 0.18f))
                        if (seed != cachedSeed || topBrush == null) {
                            cachedSeed = seed
                            // One vertical linear gradient: same look at a fraction of the fill cost
                            // of two full-screen radial gradients.
                            topBrush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            seed.hueShift(0f).copy(alpha = if (isDark) 0.44f else 0.54f),
                                            seed.hueShift(52f).copy(alpha = if (isDark) 0.34f else 0.42f),
                                            seed.hueShift(104f).copy(alpha = if (isDark) 0.24f else 0.30f),
                                        ),
                                    start = Offset.Zero,
                                    end = Offset(sizeSnapshot.width, sizeSnapshot.height),
                                )
                        }
                        topBrush?.let { drawRect(brush = it) }
                    }
                }
    )
}

private var brushSeed: Color = Color.Unspecified
/** Rotates the hue of [this] colour, keeping a HyperOS-ish saturation floor. */
private fun Color.hueShift(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    hsv[1] = hsv[1].coerceAtLeast(0.35f)
    hsv[2] = hsv[2].coerceAtLeast(0.55f)
    return Color.hsv(hsv[0], hsv[1], hsv[2])
}
internal val MiuixTopBarHeight = 56.dp
internal val MiuixCardCorner = 22.dp

/** 0f when the list is at the very top, 1f once the first item has scrolled away. */
@Composable
internal fun rememberTopBarProgress(state: LazyListState): State<Float> =
    remember(state) {
        derivedStateOf {
            when {
                state.firstVisibleItemIndex > 0 -> 1f
                else -> (state.firstVisibleItemScrollOffset / 320f).coerceIn(0f, 1f)
            }
        }
    }

/**
 * Floating frosted top bar: content scrolls underneath and the surface + title fade in as
 * [progress] goes to 1f (the Halcyon / HyperOS "progressive top bar" behaviour).
 */
@Composable
internal fun MiuixOverlayTopBar(
    title: String,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    val p = progress().coerceIn(0f, 1f)
                    drawRect(colors.surface.copy(alpha = 0.82f + 0.18f * p))
                }
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(MiuixTopBarHeight)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.width(96.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = navigationIcon,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f).graphicsLayer { alpha = if (progress() > 0.15f) 1f else 0f },
            color = colors.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.width(96.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** Halcyon-style oversized page heading, shown inside the scrolling content. */
@Composable
internal fun MiuixLargeHeader(
    title: String,
    subtitle: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp)) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = colors.onBackgroundVariant,
                fontSize = 14.sp,
            )
        }
    }
}

/** Content padding that leaves room for [MiuixOverlayTopBar] and the floating capsule bar. */
@Composable
internal fun miuixContentPadding(bottomExtra: Int = 96): PaddingValues {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(
        top = statusTop + MiuixTopBarHeight + 4.dp,
        bottom = navBottom + bottomExtra.dp,
    )
}

/** Full-screen glass canvas + scrolling content + floating top bar skeleton. */
@Composable
internal fun MiuixPageScaffold(
    topBarTitle: String,
    progress: () -> Float,
    barVisible: Boolean = true,
    scrollConnection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection? = null,
    navigationIcon: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .then(
                    if (scrollConnection != null) Modifier.nestedScroll(scrollConnection)
                    else Modifier
                )
    ) {
        MiuixGlassBackground()
        content()
        androidx.compose.animation.AnimatedVisibility(
            visible = barVisible,
            enter =
                androidx.compose.animation.slideInVertically { -it } +
                    androidx.compose.animation.fadeIn(),
            exit =
                androidx.compose.animation.slideOutVertically { -it } +
                    androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            MiuixOverlayTopBar(
                title = topBarTitle,
                progress = progress,
                navigationIcon = navigationIcon,
                actions = actions,
            )
        }
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)
        ) {
            bottomBar()
        }
    }
}

/** Halcyon-style floating capsule, used for the bottom summary / action bar. */
@Composable
internal fun MiuixCapsule(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier =
            modifier
                .padding(horizontal = 16.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(colors.surface.copy(alpha = 0.97f))
                .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/**
 * Halcyon-style frosted card: translucent surface, 20dp squircle corners, no inner margin so the
 * caller controls row padding.
 */
@Composable
internal fun MiuixGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    // Plain rounded clip + opaque fill instead of Miuix Card: the squircle path and card elevation
    // were re-clipped for every row on every frame, which showed up as main-thread jank.
    val base = modifier.clip(RoundedCornerShape(MiuixCardCorner)).background(miuixGlassColor())
    androidx.compose.foundation.layout.Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = content,
    )
}

/**
 * Halcyon-style tile: oversized bold title with the count pinned to the bottom, used for the
 * "all articles" summary card.
 */
@Composable
internal fun MiuixTile(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    MiuixGlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = title,
                color = colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle.orEmpty(),
                    modifier = Modifier.weight(1f),
                    color = colors.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.outline,
                    modifier = Modifier.width(18.dp).height(18.dp),
                )
            }
        }
    }
}

/** Halcyon-style two-line list row text: 16sp medium title plus a 13sp secondary line. */@Composable
internal fun MiuixRowText(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = title,
            color = colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = colors.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A thin divider that follows the Miuix (HyperOS) divider color. Implemented locally so the Miuix
 * skin does not depend on a specific Miuix release's divider component.
 */
@Composable
internal fun MiuixRowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(0.75.dp)
                .background(MiuixTheme.colorScheme.dividerLine)
    )
}

/**
 * A HyperOS style "title + summary + chevron" row, the equivalent of Read You's
 * `SelectableSettingGroupItem` inside the Miuix skin.
 */
@Composable
internal fun MiuixNavRow(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = summary,
        endActions = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.outline,
            )
        },
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        onClick = onClick,
    )
}

/** Rounded, frosted segmented filter bar (Halcyon's capsule segment style). */
@Composable
internal fun MiuixRoundTabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Box(modifier = modifier.clip(RoundedCornerShape(21.dp))) {
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = onTabSelected,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 21.dp,
            colors =
                TabRowDefaults.tabRowColors(
                    backgroundColor = colors.surface.copy(alpha = 0.5f),
                    contentColor = colors.onSurfaceVariantSummary,
                    selectedBackgroundColor = colors.surface,
                    selectedContentColor = colors.onSurface,
                ),
        )
    }
}

/**
 * Compact HyperOS / Halcyon top bar: small title (and optional subtitle) on the left, actions on
 * the right, frosted surface fading in with [progress].
 */
@Composable
internal fun MiuixTopBarLeft(
    title: String,
    subtitle: String? = null,
    progress: () -> Float = { 1f },
    modifier: Modifier = Modifier,
    navigationIcon: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier =
            modifier.fillMaxWidth()
                .drawBehind {
                    val p = progress().coerceIn(0f, 1f)
                    drawRect(colors.surface.copy(alpha = 0.82f + 0.18f * p))
                }
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(MiuixTopBarHeight)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon()
        Column(
            modifier =
                Modifier.weight(1f).padding(start = 8.dp).graphicsLayer {
                    // Large-title collapse: the compact bar title fades in only once the big header
                    // has scrolled away.
                    alpha = (((progress()) - 0.35f) / 0.65f).coerceIn(0f, 1f)
                }
        ) {
            Text(
                text = title,
                color = colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = colors.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/**
 * Halcyon-style bottom tab capsule: the filter switch (All / Unread / Starred) lives in a floating
 * pill bar above the gesture handle instead of a segmented control in the content.
 */
@Composable
internal fun MiuixBottomTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    MiuixCapsule {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedTabIndex
            Box(
                modifier =
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (selected) colors.primary.copy(alpha = 0.16f)
                            else Color.Transparent
                        )
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) colors.primary else colors.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Visibility state of the auto-hiding top bar (starts visible). */
@Composable
internal fun rememberAutoHideBarState(): androidx.compose.runtime.MutableState<Boolean> =
    remember { androidx.compose.runtime.mutableStateOf(true) }

/**
 * Nested-scroll connection implementing the HyperOS/Halcyon convention: scrolling down hides the
 * top bar, scrolling up brings it back. Works for any Compose scrollable placed below it.
 */
internal fun autoHideBarConnection(
    state: androidx.compose.runtime.MutableState<Boolean>
): androidx.compose.ui.input.nestedscroll.NestedScrollConnection =
    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
        ): Offset {
            val dy = available.y
            if (dy < -8f && state.value) {
                state.value = false
            } else if (dy > 8f && !state.value) {
                state.value = true
            }
            return Offset.Zero
        }
    }



