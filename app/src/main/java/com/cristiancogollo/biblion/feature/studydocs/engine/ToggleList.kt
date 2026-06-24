package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.domain.EditCommand
import com.cristiancogollo.biblion.feature.studydocs.domain.ReplaceBlockCommand
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock

/**
 * Convierte un bloque entre Paragraph y BulletList/NumberedList,
 * conservando el texto.
 *
 * Reglas:
 * - Si el bloque actual ya es del [targetType] (ej. ya es
 *   BulletList y targetType=Bullet), se convierte de vuelta a
 *   Paragraph concatenando los items con `\n`.
 * - Si el bloque es Paragraph, se convierte a la lista target
 *   con su texto como unico item.
 * - Si el bloque es del otro tipo de lista (ej. NumberedList y
 *   targetType=Bullet), se convierte a BulletList conservando
 *   los items tal cual (sin reconcatenar).
 * - Para bloques estructurales (Verse/Table/Divider/Callout),
 *   retorna `null` (no se puede togglear lista).
 *
 * El texto nunca se pierde: al volver a Paragraph se usa
 * `items.joinToString("\n")`; al pasar a lista se usa
 * `text.plain()`.
 */
fun toggleList(currentBlock: StudyBlock, targetType: ListType): EditCommand? {
    val id = currentBlock.id
    return when (currentBlock) {
        is StudyBlock.BulletList -> when (targetType) {
            ListType.Bullet -> ReplaceBlockCommand(
                targetBlockId = id,
                newBlock = StudyBlock.Paragraph(
                    id = id,
                    text = StyledText.plain(currentBlock.items.joinToString("\n")),
                ),
            )
            ListType.Numbered -> ReplaceBlockCommand(
                targetBlockId = id,
                newBlock = StudyBlock.NumberedList(
                    id = id,
                    items = currentBlock.items,
                ),
            )
        }
        is StudyBlock.NumberedList -> when (targetType) {
            ListType.Bullet -> ReplaceBlockCommand(
                targetBlockId = id,
                newBlock = StudyBlock.BulletList(
                    id = id,
                    items = currentBlock.items,
                ),
            )
            ListType.Numbered -> ReplaceBlockCommand(
                targetBlockId = id,
                newBlock = StudyBlock.Paragraph(
                    id = id,
                    text = StyledText.plain(currentBlock.items.joinToString("\n")),
                ),
            )
        }
        is StudyBlock.Paragraph -> when (targetType) {
            ListType.Bullet -> ReplaceBlockCommand(
                targetBlockId = id,
                newBlock = StudyBlock.BulletList(
                    id = id,
                    items = listOf(StyledText.plain(currentBlock.text.plain())),
                ),
            )
            ListType.Numbered -> ReplaceBlockCommand(
                targetBlockId = id,
                newBlock = StudyBlock.NumberedList(
                    id = id,
                    items = listOf(StyledText.plain(currentBlock.text.plain())),
                ),
            )
        }
        else -> null
    }
}
