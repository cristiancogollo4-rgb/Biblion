package com.cristiancogollo.biblion.feature.studydocs.ui.editor.blocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocConfig

@Composable
fun TodoListBlockEditor(
    block: StudyBlock.TodoList,
    isSelected: Boolean,
    onItemCheck: (Int, Boolean) -> Unit,
    onItemTextChange: (Int, StyledText) -> Unit,
    onAppendItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onItemFieldValueChange: (Int, TextFieldValue) -> Unit,
    onItemFocusChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = DocConfig.FontSize,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        block.items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { onItemCheck(index, it) },
                )
                Box(modifier = Modifier.weight(1f)) {
                    StyledTextEditor(
                        text = item.text,
                        onTextChange = { onItemTextChange(index, it) },
                        onFieldValueChange = { onItemFieldValueChange(index, it) },
                        onFocusChanged = { if (it) onItemFocusChanged(index) },
                        baseStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = "Tarea...",
                        isSelected = isSelected,
                        blockId = "${block.id.value}:item:$index",
                        fontSize = fontSize,
                    )
                }
                IconButton(
                    onClick = { onRemoveItem(index) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Quitar",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        TextButton(onClick = onAppendItem) {
            Text("+ Tarea")
        }
    }
}
