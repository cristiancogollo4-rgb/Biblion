package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object CursorNavigator {

    sealed interface Action {
        data class MoveFocusTo(val blockIndex: Int) : Action
        data class InsertNewBlockAfter(val blockIndex: Int) : Action
        data class MergeWithPrevious(val blockIndex: Int) : Action
        data class DegradeToParagraph(val blockIndex: Int) : Action
        data class SelectPreviousBlock(val blockIndex: Int) : Action
        data class SplitBlockAt(val blockIndex: Int, val cursorOffset: Int) : Action
        data class EscapeToParagraph(val blockIndex: Int) : Action
        data object AppendListItem : Action
        data object Handled : Action
        data object PassThrough : Action
    }

    enum class Key { ArrowUp, ArrowDown, Enter, Backspace }

    fun decide(
        key: Key,
        blockIndex: Int,
        offset: Int,
        textLength: Int,
        isTextBlock: Boolean,
        isListBlock: Boolean,
        hasNextBlock: Boolean,
        hasPrevBlock: Boolean,
        isShiftPressed: Boolean,
        isEmpty: Boolean = false,
        isDegradableSpecial: Boolean = false,
        isPrevImmutable: Boolean = false,
    ): Action {
        if (key == Key.ArrowUp && offset == 0 && hasPrevBlock) {
            return Action.MoveFocusTo(blockIndex - 1)
        }
        if (key == Key.ArrowDown && offset == textLength && hasNextBlock) {
            return Action.MoveFocusTo(blockIndex + 1)
        }
        if (key == Key.Enter) {
            if (isShiftPressed) return Action.PassThrough
            if (isEmpty && isDegradableSpecial) return Action.EscapeToParagraph(blockIndex)
            if (offset > 0 && offset < textLength) return Action.SplitBlockAt(blockIndex, offset)
            if (offset == textLength) {
                if (isListBlock) return Action.AppendListItem
                if (isTextBlock) return Action.InsertNewBlockAfter(blockIndex)
            }
        }
        if (key == Key.Backspace && offset == 0 && hasPrevBlock) {
            if (isPrevImmutable) return Action.SelectPreviousBlock(blockIndex - 1)
            if (isDegradableSpecial) return Action.DegradeToParagraph(blockIndex)
            if (isTextBlock) return Action.MergeWithPrevious(blockIndex)
        }
        return Action.PassThrough
    }

    fun blockCategory(text: StyledText?, isList: Boolean): String {
        if (isList) return "list"
        if (text != null) return "text"
        return "other"
    }
}
