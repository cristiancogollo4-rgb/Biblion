package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

@Composable
fun SaveTeachingDialog(
    currentTitle: String,
    currentTags: List<String>,
    onSave: (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    saveError: String? = null,
) {
    var title by remember { mutableStateOf(currentTitle) }
    var tagsInput by remember { mutableStateOf(currentTags.joinToString(", ")) }
    var pendingCustomTag by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var validationGroupIndex by remember { mutableStateOf<Int?>(null) }
    val shortScreen = LocalConfiguration.current.screenHeightDp < 500

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).widthIn(max = 620.dp).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp,
        ) {
            Column {
                Surface(color = BiblionBluePrimary) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(13.dp),
                            color = BiblionGoldPrimary,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Save, contentDescription = null, tint = BiblionBluePrimary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.study_save_eyebrow),
                                style = MaterialTheme.typography.labelSmall,
                                color = BiblionGoldPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(R.string.study_save_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            if (!shortScreen) {
                                Text(
                                    text = stringResource(R.string.study_save_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.82f),
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, enabled = !isSaving) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.study_tags_close), tint = Color.White)
                        }
                    }
                }

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                titleError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.study_save_document_name)) },
                            placeholder = { Text(stringResource(R.string.study_save_title_hint)) },
                            supportingText = if (titleError) {
                                { Text(stringResource(R.string.study_save_title_required)) }
                            } else null,
                            isError = titleError,
                            enabled = !isSaving,
                            singleLine = true,
                        )

                        StudyTagSelector(
                            value = tagsInput,
                            onValueChange = {
                                tagsInput = it
                                validationGroupIndex = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isCompact = shortScreen,
                            onPendingCustomTagChange = { pendingCustomTag = it },
                            validationGroupIndex = validationGroupIndex,
                            enabled = !isSaving,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                    if (saveError != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.study_save_failed),
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    } else if (!shortScreen) {
                        Text(
                            text = stringResource(R.string.study_save_visibility_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss, enabled = !isSaving) {
                            Text(stringResource(R.string.study_save_cancel))
                        }
                        Button(
                            onClick = {
                                val parsed = studyTagsForSave(tagsInput, pendingCustomTag)
                                titleError = title.isBlank()
                                validationGroupIndex = firstIncompleteStudyTagGroupIndex(parsed)
                                if (!titleError && validationGroupIndex == null) onSave(title.trim(), parsed)
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BiblionBluePrimary,
                                contentColor = Color.White,
                            ),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(if (isSaving) R.string.study_tags_saving else R.string.study_save_action))
                        }
                    }
                }
            }
        }
    }
}
