package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DocConfig {
    val EditorMaxWidth = 680.dp
    val EditorHorizontalPadding = 24.dp
    val EditorContentVerticalPadding = 16.dp
    val BlockGap = 8.dp

    val FontSize = 11.sp
    val LineHeight = 15.sp

    val Heading1Size = 22.sp
    val Heading2Size = 18.sp
    val Heading3Size = 14.sp

    val FontSizeLevels = listOf(9f, 10f, 11f, 12f, 13f, 14f, 16f, 18f, 20f, 22f)
    const val DefaultFontSizeIndex = 2

    // Legacy constants for PDF export / DocumentPagination
    val PageWidth = 560.dp
    val PageHeight = 900.dp
    val PagePadding = 64.dp
    val PageContentVerticalPadding = 80.dp
    val PageGap = 16.dp

    // Zoom constants (used by CameraState / read screen)
    const val MinZoom = 0.50f
    const val MaxZoom = 2.00f
    val ZoomLevels = listOf(0.50f, 0.75f, 0.90f, 1.00f, 1.25f, 1.50f, 2.00f)
}
