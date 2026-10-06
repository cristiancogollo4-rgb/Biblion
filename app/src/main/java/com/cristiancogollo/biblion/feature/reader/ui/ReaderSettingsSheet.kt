package com.cristiancogollo.biblion.feature.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.reader.READER_MAX_LINE_SPACING
import com.cristiancogollo.biblion.feature.reader.READER_MAX_TEXT_WIDTH_DP
import com.cristiancogollo.biblion.feature.reader.READER_MIN_LINE_SPACING
import com.cristiancogollo.biblion.feature.reader.READER_MIN_TEXT_WIDTH_DP
import com.cristiancogollo.biblion.feature.reader.ReaderFontFamily
import com.cristiancogollo.biblion.feature.reader.ReaderPreferences
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    fontSizeSp: Float,
    preferences: ReaderPreferences,
    onDismiss: () -> Unit,
    onApply: (Float, ReaderPreferences) -> Unit,
) {
    var draftFontSize by remember(fontSizeSp) { mutableFloatStateOf(fontSizeSp) }
    var draft by remember(preferences) { mutableStateOf(preferences.normalized()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.reader_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.reader_settings_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // La muestra queda fuera del contenedor desplazable: nunca desaparece al ajustar.
                ReaderPreview(
                    fontSizeSp = draftFontSize,
                    preferences = draft,
                )

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SettingsHeading(stringResource(R.string.reader_settings_text))
                    SliderSettingLabel(
                        title = stringResource(R.string.reader_font_size),
                        value = stringResource(
                            R.string.reader_font_size_value,
                            draftFontSize.roundToInt(),
                        ),
                    )
                    Slider(
                        value = draftFontSize.coerceIn(12f, 35f),
                        onValueChange = { draftFontSize = it },
                        valueRange = 12f..35f,
                        steps = 22,
                    )

                    ChoiceRow(
                        options = listOf(
                            ReaderFontFamily.SERIF to stringResource(R.string.reader_font_serif),
                            ReaderFontFamily.SANS_SERIF to stringResource(R.string.reader_font_sans),
                            ReaderFontFamily.ACCESSIBLE to stringResource(R.string.reader_font_accessible),
                        ),
                        selected = draft.fontFamily,
                        onSelected = { draft = draft.copy(fontFamily = it) },
                    )

                    SliderSettingLabel(
                        title = stringResource(R.string.reader_line_spacing),
                        value = stringResource(
                            R.string.reader_line_spacing_value,
                            draft.lineSpacingMultiplier,
                        ),
                    )
                    Slider(
                        value = draft.lineSpacingMultiplier,
                        onValueChange = {
                            draft = draft.copy(lineSpacingMultiplier = it)
                        },
                        valueRange = READER_MIN_LINE_SPACING..READER_MAX_LINE_SPACING,
                    )

                    SliderSettingLabel(
                        title = stringResource(R.string.reader_text_width),
                        value = stringResource(
                            R.string.reader_text_width_value,
                            draft.textWidthDp.roundToInt(),
                        ),
                    )
                    Slider(
                        value = draft.textWidthDp,
                        onValueChange = { draft = draft.copy(textWidthDp = it) },
                        valueRange = READER_MIN_TEXT_WIDTH_DP..READER_MAX_TEXT_WIDTH_DP,
                    )

                    HorizontalDivider()
                    SettingsHeading(stringResource(R.string.reader_settings_flow))
                    SettingsSwitch(
                        title = stringResource(R.string.reader_continuous_title),
                        description = stringResource(R.string.reader_continuous_description),
                        checked = draft.continuousScrolling,
                        onCheckedChange = { draft = draft.copy(continuousScrolling = it) },
                    )

                    HorizontalDivider()
                    SettingsHeading(stringResource(R.string.reader_settings_accessibility))
                    SettingsSwitch(
                        title = stringResource(R.string.reader_show_headings),
                        checked = draft.showSectionHeadings,
                        onCheckedChange = { draft = draft.copy(showSectionHeadings = it) },
                    )
                    SettingsSwitch(
                        title = stringResource(R.string.reader_show_verse_numbers),
                        checked = draft.showVerseNumbers,
                        onCheckedChange = { draft = draft.copy(showVerseNumbers = it) },
                    )
                    SettingsSwitch(
                        title = stringResource(R.string.reader_high_contrast),
                        description = stringResource(R.string.reader_high_contrast_description),
                        checked = draft.highContrast,
                        onCheckedChange = { draft = draft.copy(highContrast = it) },
                    )
                    SettingsSwitch(
                        title = stringResource(R.string.reader_reduce_motion),
                        checked = draft.reduceMotion,
                        onCheckedChange = { draft = draft.copy(reduceMotion = it) },
                    )
                    Spacer(Modifier.height(4.dp))
                }

                Button(
                    onClick = {
                        onApply(
                            draftFontSize,
                            draft.normalized(),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.action_apply_reader_settings))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ReaderPreview(
    fontSizeSp: Float,
    preferences: ReaderPreferences,
) {
    val widthFraction = (
        0.58f +
            0.42f *
            (preferences.textWidthDp - READER_MIN_TEXT_WIDTH_DP) /
            (READER_MAX_TEXT_WIDTH_DP - READER_MIN_TEXT_WIDTH_DP)
        ).coerceIn(0.58f, 1f)
    val textStyle = TextStyle(
        fontFamily = preferences.fontFamily.asComposeFontFamily(),
        fontWeight = preferences.fontFamily.bodyWeight(),
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * preferences.lineSpacingMultiplier).sp,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 190.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .heightIn(min = 150.dp, max = 190.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (preferences.showSectionHeadings) {
                    Text(
                        text = stringResource(R.string.reader_preview_heading),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (preferences.highContrast) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                val previewVerses = listOf(
                    "1" to stringResource(R.string.reader_preview_verse),
                    "2" to stringResource(R.string.reader_preview_verse_second),
                    "3" to stringResource(R.string.reader_preview_verse_third),
                )
                previewVerses.forEach { (number, text) ->
                    PreviewVerse(
                        number = number,
                        text = text,
                        style = textStyle,
                        showNumber = preferences.showVerseNumbers,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewVerse(
    number: String,
    text: String,
    style: TextStyle,
    showNumber: Boolean,
) {
    Text(
        text = if (showNumber) "$number  $text" else text,
        style = style,
    )
}

@Composable
private fun SliderSettingLabel(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label, maxLines = 1) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
