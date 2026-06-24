package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocJson
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
sealed interface PersistableEditCommand {
    val commandType: String
    fun toCommand(): EditCommand

    @Serializable
    data class Insert(
        val atIndex: Int,
        val blockId: String,
        val blockJson: String,
    ) : PersistableEditCommand {
        override val commandType = "Insert"
        override fun toCommand(): EditCommand {
            val block = StudyDocJson.decodeBlock(blockJson) ?: StudyBlock.Paragraph(id = BlockId(blockId))
            return InsertBlockCommand(atIndex = atIndex, block = block)
        }
    }

    @Serializable
    data class Delete(val blockId: String) : PersistableEditCommand {
        override val commandType = "Delete"
        override fun toCommand(): EditCommand = DeleteBlockCommand(blockId = BlockId(blockId))
    }

    @Serializable
    data class DeleteWithSnapshot(
        val blockJson: String,
        val originalIndex: Int,
    ) : PersistableEditCommand {
        override val commandType = "DeleteWithSnapshot"
        override fun toCommand(): EditCommand {
            val block = StudyDocJson.decodeBlock(blockJson) ?: StudyBlock.Paragraph(id = BlockId("missing"))
            return DeleteBlockCommandWithSnapshot(block = block, originalIndex = originalIndex)
        }
    }

    @Serializable
    data class UpdateTitle(
        val newTitle: String,
        val oldTitle: String,
    ) : PersistableEditCommand {
        override val commandType = "UpdateTitle"
        override fun toCommand(): EditCommand = UpdateTitleCommand(newTitle = newTitle, oldTitle = oldTitle)
    }

    @Serializable
    data class Move(
        val fromIndex: Int,
        val toIndex: Int,
    ) : PersistableEditCommand {
        override val commandType = "Move"
        override fun toCommand(): EditCommand = MoveBlockCommand(fromIndex = fromIndex, toIndex = toIndex)
    }
}

fun EditCommand.toPersistable(): PersistableEditCommand? = when (this) {
    is InsertBlockCommand -> PersistableEditCommand.Insert(
        atIndex = atIndex,
        blockId = block.id.value,
        blockJson = StudyDocJson.encodeBlock(block),
    )
    is DeleteBlockCommand -> PersistableEditCommand.Delete(blockId = blockId.value)
    is DeleteBlockCommandWithSnapshot -> PersistableEditCommand.DeleteWithSnapshot(
        blockJson = StudyDocJson.encodeBlock(block),
        originalIndex = originalIndex,
    )
    is RestoreBlockCommand -> null
    is UpdateTitleCommand -> PersistableEditCommand.UpdateTitle(
        newTitle = newTitle,
        oldTitle = oldTitle,
    )
    is MoveBlockCommand -> PersistableEditCommand.Move(
        fromIndex = fromIndex,
        toIndex = toIndex,
    )
    is MergeBlocksCommand -> null
    is SplitBlocksCommand -> null
    is ReplaceBlockCommand -> null
    is RestoreOriginalBlockCommand -> null
}

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
object PersistableEditCommandSerializer : KSerializer<PersistableEditCommand> {
    override val descriptor = buildSerialDescriptor("PersistableEditCommand", PolymorphicKind.SEALED) {
        element<String>("commandType")
    }

    override fun serialize(encoder: Encoder, value: PersistableEditCommand) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("PersistableEditCommandSerializer solo soporta JsonEncoder")
        val inner: JsonObject = when (value) {
            is PersistableEditCommand.Insert -> JsonObject(
                mapOf(
                    "atIndex" to JsonPrimitive(value.atIndex),
                    "blockId" to JsonPrimitive(value.blockId),
                    "blockJson" to JsonPrimitive(value.blockJson),
                ),
            )
            is PersistableEditCommand.Delete -> JsonObject(
                mapOf("blockId" to JsonPrimitive(value.blockId)),
            )
            is PersistableEditCommand.DeleteWithSnapshot -> JsonObject(
                mapOf(
                    "blockJson" to JsonPrimitive(value.blockJson),
                    "originalIndex" to JsonPrimitive(value.originalIndex),
                ),
            )
            is PersistableEditCommand.UpdateTitle -> JsonObject(
                mapOf(
                    "newTitle" to JsonPrimitive(value.newTitle),
                    "oldTitle" to JsonPrimitive(value.oldTitle),
                ),
            )
            is PersistableEditCommand.Move -> JsonObject(
                mapOf(
                    "fromIndex" to JsonPrimitive(value.fromIndex),
                    "toIndex" to JsonPrimitive(value.toIndex),
                ),
            )
        }
        val combined = JsonObject(
            mapOf("commandType" to JsonPrimitive(value.commandType)) + inner,
        )
        jsonEncoder.encodeJsonElement(combined)
    }

    override fun deserialize(decoder: Decoder): PersistableEditCommand {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("PersistableEditCommandSerializer solo soporta JsonDecoder")
        val element: JsonElement = jsonDecoder.decodeJsonElement()
        val obj = element as? JsonObject ?: error("Esperaba JsonObject, recibi ${element::class}")
        val commandType = (obj["commandType"] as? JsonPrimitive)?.contentOrNullSafe()
            ?: error("Falta commandType")
        val data = obj.toMutableMap().also { it.remove("commandType") }
        val dataObj = JsonObject(data)
        val dataJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        return when (commandType) {
            "Insert" -> dataJson.decodeFromJsonElement(PersistableEditCommand.Insert.serializer(), dataObj)
            "Delete" -> dataJson.decodeFromJsonElement(PersistableEditCommand.Delete.serializer(), dataObj)
            "DeleteWithSnapshot" -> dataJson.decodeFromJsonElement(PersistableEditCommand.DeleteWithSnapshot.serializer(), dataObj)
            "UpdateTitle" -> dataJson.decodeFromJsonElement(PersistableEditCommand.UpdateTitle.serializer(), dataObj)
            "Move" -> dataJson.decodeFromJsonElement(PersistableEditCommand.Move.serializer(), dataObj)
            else -> error("commandType desconocido: $commandType")
        }
    }
}

private fun JsonPrimitive.contentOrNullSafe(): String? = if (isString) content else null
