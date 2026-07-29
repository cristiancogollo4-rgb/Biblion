package com.cristiancogollo.biblion.core.ui.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object BiblionMotion {
    const val QUICK_MS = 180
    const val STANDARD_MS = 260
    const val EMPHASIS_MS = 360
}

@Composable
fun rememberBiblionMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }
}
