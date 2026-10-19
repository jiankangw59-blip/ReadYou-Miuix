package me.ash.reader.ui.miuix

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.ash.reader.R
import me.ash.reader.infrastructure.preference.LocalUiStyle
import me.ash.reader.infrastructure.preference.UiStylePreference
import me.ash.reader.infrastructure.preference.isMiuix
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * HyperOS / MIUI style settings page, styled after Halcyon: floating bar, oversized heading and
 * card groups. It hosts the switch that flips the whole app between the Material You skin and the
 * Miuix (HyperOS) skin, plus shortcuts into the existing Read You settings pages.
 */
@Composable
fun MiuixSettingsPage(
    onBack: () -> Unit = {},
    navigateToAccounts: () -> Unit = {},
    navigateToColorAndStyle: () -> Unit = {},
    navigateToInteraction: () -> Unit = {},
    navigateToLanguages: () -> Unit = {},
    navigateToTipsAndSupport: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiStyle = LocalUiStyle.current
    var miuixEnabled by remember(uiStyle) { mutableStateOf(uiStyle.isMiuix) }

    val listState = rememberLazyListState()
    ProvideMiuixSeed(rememberWallpaperSeed())
    val barVisible = rememberAutoHideBarState()
    var showMarkdownPreview by remember { mutableStateOf(false) }
    val barScrollConnection = remember(barVisible) { autoHideBarConnection(barVisible) }
    val progress = rememberTopBarProgress(listState)

    MiuixPageScaffold(
        topBarTitle = stringResource(R.string.settings),
        progress = { progress.value },
        barVisible = barVisible.value,
        scrollConnection = barScrollConnection,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = miuixContentPadding(),
        ) {
            item { MiuixLargeHeader(title = stringResource(R.string.settings)) }

            item {
                SmallTitle(
                    text = stringResource(R.string.ui_style),
                    insideMargin =
                        PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                )
            }

            item {
                MiuixGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                ) {
                    SwitchPreference(
                        title = stringResource(R.string.ui_style),
                        summary = stringResource(R.string.ui_style_switch_desc),
                        checked = miuixEnabled,
                        onCheckedChange = { checked ->
                            miuixEnabled = checked
                            val style =
                                if (checked) UiStylePreference.Miuix
                                else UiStylePreference.Material
                            style.put(context, scope)
                        },
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            item {
                SmallTitle(
                    text = stringResource(R.string.settings),
                    insideMargin =
                        PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                )
            }

            item {
                MiuixGlassCard(
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                ) {
                    MiuixNavRow(
                        title = stringResource(R.string.accounts),
                        summary = stringResource(R.string.accounts_desc),
                        onClick = navigateToAccounts,
                    )
                    MiuixRowDivider(modifier = Modifier.padding(start = 16.dp))
                    MiuixNavRow(
                        title = stringResource(R.string.color_and_style),
                        summary = stringResource(R.string.color_and_style_desc),
                        onClick = navigateToColorAndStyle,
                    )
                    MiuixRowDivider(modifier = Modifier.padding(start = 16.dp))
                    MiuixNavRow(
                        title = stringResource(R.string.interaction),
                        summary = stringResource(R.string.interaction_desc),
                        onClick = navigateToInteraction,
                    )
                    MiuixRowDivider(modifier = Modifier.padding(start = 16.dp))
                    MiuixNavRow(
                        title = stringResource(R.string.languages),
                        onClick = navigateToLanguages,
                    )
                    MiuixRowDivider(modifier = Modifier.padding(start = 16.dp))
                    MiuixNavRow(
                        title = "Markdown 渲染预览",
                        summary = "检查标题 / 引用 / 代码块 / 表格排版",
                        onClick = { showMarkdownPreview = true },
                    )
                    MiuixRowDivider(modifier = Modifier.padding(start = 16.dp))
                    MiuixNavRow(
                        title = stringResource(R.string.tips_and_support),
                        summary = stringResource(R.string.tips_and_support_desc),
                        onClick = navigateToTipsAndSupport,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showMarkdownPreview) {
        MiuixMarkdownPreview(onClose = { showMarkdownPreview = false })
    }
}
