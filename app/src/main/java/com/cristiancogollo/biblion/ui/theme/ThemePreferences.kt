package com.cristiancogollo.biblion

import android.content.Context
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode

object ThemePreferences {
    private const val KEY_THEME_MODE = "biblion_theme_mode"

    fun isDarkModeEnabled(context: Context, defaultValue: Boolean): Boolean {
        return AppPreferencesSyncStore.getDarkModeEnabled(context, defaultValue)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        AppPreferencesSyncStore.setDarkModeEnabled(context, enabled)
    }

    fun getThemeMode(context: Context, defaultValue: BiblionThemeMode = BiblionThemeMode.LIGHT): BiblionThemeMode {
        val stored = context.getSharedPreferences(AppPreferencesSyncStore.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)
        return stored?.let { value -> BiblionThemeMode.entries.firstOrNull { it.name == value } } ?: defaultValue
    }

    fun setThemeMode(context: Context, mode: BiblionThemeMode) {
        context.getSharedPreferences(AppPreferencesSyncStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .putBoolean(AppPreferencesSyncStore.KEY_DARK_MODE_ENABLED, mode == BiblionThemeMode.DARK)
            .apply()
    }
}
