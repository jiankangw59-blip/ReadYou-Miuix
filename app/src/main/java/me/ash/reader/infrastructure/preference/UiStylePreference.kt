package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.uiStyle
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

/**
 * Which UI skin is used to render the app.
 *
 * - [Material]: the original Read You Material You (Material 3) interface.
 * - [Miuix]: a HyperOS / MIUI style interface built with the Miuix Compose library.
 *
 * Only the presentation layer is affected: parsing, sync and the database are shared by both
 * skins, because both skins observe the very same view models.
 */
val LocalUiStyle = compositionLocalOf<UiStylePreference> { UiStylePreference.Material }

sealed class UiStylePreference(val value: Int) : Preference() {

    object Material : UiStylePreference(0)

    object Miuix : UiStylePreference(1)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch { context.dataStore.put(uiStyle, value) }
    }

    fun toDesc(context: Context): String =
        when (this) {
            Material -> context.getString(R.string.ui_style_material)
            Miuix -> context.getString(R.string.ui_style_miuix)
        }

    companion object {

        val default = Material
        val values = listOf(Material, Miuix)

        @Suppress("UNCHECKED_CAST")
        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[uiStyle]?.key as Preferences.Key<Int>]) {
                1 -> Miuix
                else -> default
            }
    }
}

val UiStylePreference.isMiuix: Boolean
    get() = this is UiStylePreference.Miuix

operator fun UiStylePreference.not(): UiStylePreference =
    when (this) {
        UiStylePreference.Material -> UiStylePreference.Miuix
        UiStylePreference.Miuix -> UiStylePreference.Material
    }
