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
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary

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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
        textColor,
    ) {
        buildAnnotatedString {
            section.verses.forEachIndexed { index, (verseNumber, verseText) ->
                val start = length
                val background = if (verseNumber in selectedVerseNumbers) {
                    BiblionBluePrimary.copy(alpha = 0.16f)
                } else {
                    highlightColors[verseNumber] ?: Color.Transparent
                }
                if (showVerseNumbers) {
                    withStyle(
                        SpanStyle(
                            fontSize = (fontSize.value * 0.6f).sp,
                            fontWeight = FontWeight.Bold,
                            baselineShift = BaselineShift.Superscript,
                            color = primaryColor,
                            background = background,
                        )
                    ) {
                        append(verseNumber)
                    }
                    append(" ")
                }
                withStyle(
                    SpanStyle(
                        color = textColor,
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
