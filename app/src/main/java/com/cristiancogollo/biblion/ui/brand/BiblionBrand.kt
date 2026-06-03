package com.cristiancogollo.biblion

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

fun biblionLogoRes(isDarkTheme: Boolean): Int {
    return if (isDarkTheme) R.drawable.logo_biblion_dark else R.drawable.logo_biblion_light
}

@Composable
fun biblionLogoResForCurrentTheme(): Int {
    return biblionLogoRes(MaterialTheme.colorScheme.background.luminance() < 0.5f)
}

object BiblionLauncherIconManager {
    private const val TAG = "BiblionLauncherIcon"

    fun applyThemeIcon(context: Context, isDarkTheme: Boolean) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val lightAlias = ComponentName(appContext, "${appContext.packageName}.MainActivityLight")
        val darkAlias = ComponentName(appContext, "${appContext.packageName}.MainActivityDark")
        val enabledAlias = if (isDarkTheme) darkAlias else lightAlias
        val disabledAlias = if (isDarkTheme) lightAlias else darkAlias

        runCatching {
            packageManager.setComponentEnabledSetting(
                enabledAlias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                disabledAlias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }.onFailure { error ->
            Log.w(TAG, "No se pudo actualizar el icono del launcher.", error)
        }
    }
}
