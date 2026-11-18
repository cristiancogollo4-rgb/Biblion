package com.cristiancogollo.biblion.feature.reader.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.reader.BibleTextSection
import com.cristiancogollo.biblion.feature.reader.ReaderVerseNumberFont
import com.cristiancogollo.biblion.feature.reader.readerSectionHeadingStyle
import com.cristiancogollo.biblion.feature.reader.readerVerseForeground
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.biblionLabelColor

private const val VERSE_ANNOTATION_TAG = "biblion_verse"

@Suppress("DEPRECATION")
@Composable
fun FlowingBibleSection(
    section: BibleTextSection,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    lineSpacingMultiplier: Float,
    showVerseNumbers: Boolean,
    showHeading: Boolean,
    highContrast: Boolean,
    selectedVerseNumbers: Set<String>,
    highlightColors: Map<String, Color>,
    onVerseClick: ((verseNumber: String, verseText: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (showHeading && !section.heading.isNullOrBlank()) {
        Text(
            text = section.heading,
            style = readerSectionHeadingStyle(fontFamily),
            color = if (highContrast) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() }
                .padding(top = 6.dp, bottom = 10.dp),
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val selectionColor = primaryColor.copy(alpha = 0.18f)
    val themeMode = LocalBiblionThemeMode.current
    val numberColor = biblionLabelColor(themeMode)
    val accessibilityDescription = stringResource(
        R.string.reader_flowing_section_accessibility,
        section.verses.joinToString(" ") { (number, text) -> "$number $text" },
    )
    val annotatedText = remember(
        section,
        fontSize,
        showVerseNumbers,
        selectedVerseNumbers,
        highlightColors,
        primaryColor,
        numberColor,
        textColor,
        themeMode,
    ) {
        buildAnnotatedString {
            section.verses.forEachIndexed { index, (verseNumber, verseText) ->
                val start = length
                val isSelected = verseNumber in selectedVerseNumbers
                val highlight = highlightColors[verseNumber] ?: Color.Transparent
                val background = if (isSelected) selectionColor else highlight
                val verseTextColor = readerVerseForeground(highlight, textColor, themeMode, isSelected)
                val verseNumberColor = readerVerseForeground(highlight, numberColor, themeMode, isSelected)
                if (showVerseNumbers) {
                    withStyle(
                        SpanStyle(
                            fontSize = (fontSize.value * 0.6f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ReaderVerseNumberFont,
                            baselineShift = BaselineShift.Superscript,
                            color = verseNumberColor,
                            background = background,
                        )
                    ) {
                        append(verseNumber)
                    }
                    append(" ")
                }
                withStyle(
                    SpanStyle(
                        color = verseTextColor,
                        background = background,
                    )
                ) {
                    append(verseText.trim())
                }
                if (index < section.verses.lastIndex) append(" ")
                val end = length
                addStringAnnotation(
                    tag = VERSE_ANNOTATION_TAG,
                    annotation = verseNumber,
                    start = start,
                    end = end,
                )
            }
        }
    }
    val textStyle = MaterialTheme.typography.bodyLarge.merge(
        TextStyle(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize,
            lineHeight = (fontSize.value * lineSpacingMultiplier).sp,
            color = textColor,
        )
    )
    val textModifier = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {
            contentDescription = accessibilityDescription
        }
        .padding(horizontal = 8.dp, vertical = 8.dp)
        .padding(bottom = 12.dp)

    if (onVerseClick == null) {
        Text(
            text = annotatedText,
            style = textStyle,
            modifier = textModifier,
        )
    } else {
        ClickableText(
            text = annotatedText,
            style = textStyle,
            modifier = textModifier,
            onClick = { offset ->
                val verseNumber = annotatedText
                    .getStringAnnotations(VERSE_ANNOTATION_TAG, offset, offset)
                    .firstOrNull()
                    ?.item
                    ?: return@ClickableText
                val verseText = section.verses
                    .firstOrNull { it.first == verseNumber }
                    ?.second
                    ?: return@ClickableText
                onVerseClick(verseNumber, verseText)
            },
        )
    }
}
