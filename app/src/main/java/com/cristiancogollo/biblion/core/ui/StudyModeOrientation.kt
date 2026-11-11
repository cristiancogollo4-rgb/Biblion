package com.cristiancogollo.biblion.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Keeps Study Mode in either landscape direction on tablets/foldables
 * (smallest width >= 600dp) and restores the previous orientation policy when
 * its owner leaves the composition. Phones keep free rotation: a forced
 * landscape plus the IME leaves only a few lines to write.
 */
@Composable
fun StudyModeLandscapeLock(enabled: Boolean = true) {
    val activity = LocalContext.current.findActivity()
    val isLargeScreen = LocalConfiguration.current.smallestScreenWidthDp >= 600

    DisposableEffect(activity, enabled, isLargeScreen) {
        if (!enabled || !isLargeScreen || activity == null) {
            onDispose { }
        } else {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            onDispose {
                if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    activity.requestedOrientation = previousOrientation
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
