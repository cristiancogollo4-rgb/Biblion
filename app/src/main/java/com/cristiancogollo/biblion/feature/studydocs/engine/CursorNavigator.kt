package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object CursorNavigator {

    sealed interface Action {
        data class MoveFocusTo(val blockIndex: Int) : Action
        data class InsertNewBlockAfter(val blockIndex: Int) : Action
        data object AppendListItem : Action
        data object Handled : Action
        data object PassThrough : Action
    }

    enum class Key { ArrowUp, ArrowDown, Enter }

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
    ): Action {
        if (key == Key.ArrowUp && offset == 0 && hasPrevBlock) {
            return Action.MoveFocusTo(blockIndex - 1)
        }
        if (key == Key.ArrowDown && offset == textLength && hasNextBlock) {
            return Action.MoveFocusTo(blockIndex + 1)
        }
        if (key == Key.Enter && offset == textLength) {
            if (isShiftPressed) return Action.PassThrough
            if (isListBlock) return Action.AppendListItem
            if (isTextBlock) return Action.InsertNewBlockAfter(blockIndex)
            return Action.PassThrough
        }
        return Action.PassThrough
    }

    fun blockCategory(text: StyledText?, isList: Boolean): String {
        if (isList) return "list"
        if (text != null) return "text"
        return "other"
    }
}
