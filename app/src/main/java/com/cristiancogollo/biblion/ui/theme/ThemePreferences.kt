package com.cristiancogollo.biblion.ui.theme

import android.content.Context

private const val PREFS_NAME = "BiblionAppPrefs"
private const val KEY_DARK_MODE_ENABLED = "darkModeEnabled"

object ThemePreferences {
    fun isDarkModeEnabled(context: Context, defaultValue: Boolean): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_MODE_ENABLED, defaultValue)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE_ENABLED, enabled)
            .apply()
    }
}
