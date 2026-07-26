package com.cristiancogollo.biblion.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Keeps Study Mode in either landscape direction and restores the previous
 * orientation policy when its owner leaves the composition.
 */
@Composable
fun StudyModeLandscapeLock(enabled: Boolean = true) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, enabled) {
        if (!enabled || activity == null) {
            onDispose { }
        } else {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            Log.d(
                STUDY_ORIENTATION_LOG,
                "lock previous=$previousOrientation current=${activity.requestedOrientation}",
            )

            onDispose {
                if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    activity.requestedOrientation = previousOrientation
                    Log.d(
                        STUDY_ORIENTATION_LOG,
                        "restore orientation=$previousOrientation",
                    )
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

private const val STUDY_ORIENTATION_LOG = "BIBLION_STUDY_ORIENTATION"
