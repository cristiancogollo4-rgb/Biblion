package com.cristiancogollo.biblion

import android.content.Context

object ThemePreferences {
    fun isDarkModeEnabled(context: Context, defaultValue: Boolean): Boolean {
        return AppPreferencesSyncStore.getDarkModeEnabled(context, defaultValue)
    }

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        AppPreferencesSyncStore.setDarkModeEnabled(context, enabled)
    }
}
