package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig

internal val GoogleDocsFontSizes = listOf(
    8, 9, 10, 11, 12, 14, 16, 18, 20, 24, 30, 36, 48, 60, 72,
)

@Composable
internal fun FontSizeControl(
    currentFontSize: Int,
    onStepFontSize: (Int) -> Unit,
    onSetFontSize: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }
    var draft by remember(currentFontSize) {
        mutableStateOf(currentFontSize.takeIf { it >= 0 }?.toString().orEmpty())
    }

    fun commitDraft() {
        val parsed = draft.toIntOrNull() ?: return
        val normalized = parsed.coerceIn(DocConfig.MIN_FONT_SIZE, DocConfig.MAX_FONT_SIZE)
        draft = normalized.toString()
        onSetFontSize(normalized)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(
            onClick = { onStepFontSize(-1) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = "Reducir tamano de texto",
                modifier = Modifier.size(18.dp),
            )
        }

        Box {
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .width(70.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(4.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { candidate ->
                        if (candidate.length <= 3 && candidate.all(Char::isDigit)) {
                            draft = candidate
                        }
                    },
                    modifier = Modifier
                        .width(42.dp)
                        .padding(start = 6.dp)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && draft.isNotBlank()) commitDraft()
                        }
                        .semantics { contentDescription = "Tamano de texto" },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            commitDraft()
                            focusManager.clearFocus()
                        },
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = "Escoger tamano de texto",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                GoogleDocsFontSizes.forEach { size ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = size.toString(),
                                color = if (size == currentFontSize) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        },
                        onClick = {
                            draft = size.toString()
                            expanded = false
                            onSetFontSize(size)
                        },
                    )
                }
            }
        }

        IconButton(
            onClick = { onStepFontSize(1) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Aumentar tamano de texto",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
