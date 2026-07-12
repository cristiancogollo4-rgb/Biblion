package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.VerseRef

sealed class StudyTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
) {
    abstract fun generateInitialBlocks(): List<StudyBlock>
    abstract fun generateMetadata(): DocMetadata

    object ExpositorySermon : StudyTemplate(
        id = "expository_sermon",
        name = "Predicación Expositiva",
        description = "Estructura para predicación versículo por versículo",
        icon = "📖",
    ) {
        override fun generateInitialBlocks(): List<StudyBlock> = listOf(
            StudyBlock.Heading(
                level = 1,
                text = StyledText.plain("Texto Base"),
            ),
            StudyBlock.Verse(
                reference = VerseRef(
                    book = "Génesis",
                    chapter = 1,
                    verseStart = 1,
                    verseEnd = 3,
                    version = "RVR1960",
                ),
                primaryText = StyledText.plain("1 En el principio creó Dios los cielos y la tierra..."),
                primaryVersion = "RVR1960",
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Introducción"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Contexto histórico y propósito del pasaje"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Punto 1"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Explicación del primer punto"),
            ),
            StudyBlock.Note(
                text = StyledText.plain("Notas adicionales"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Punto 2"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Explicación del segundo punto"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Punto 3"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Explicación del tercer punto"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Aplicación"),
            ),
            StudyBlock.Reflection(
                prompt = "¿Cómo aplicar esto en tu vida?",
                text = StyledText.plain("Reflexión personal"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Conclusión"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Resumen y llamado a la acción"),
            ),
        )

        override fun generateMetadata(): DocMetadata = DocMetadata(
            tags = listOf("predicacion", "iglesia"),
        )
    }

    object Devotional : StudyTemplate(
        id = "devotional",
        name = "Devocional",
        description = "Reflexión personal y aplicación práctica",
        icon = "🙏",
    ) {
        override fun generateInitialBlocks(): List<StudyBlock> = listOf(
            StudyBlock.Heading(
                level = 1,
                text = StyledText.plain("Versículo del Día"),
            ),
            StudyBlock.Verse(
                reference = VerseRef(
                    book = "Salmos",
                    chapter = 23,
                    verseStart = 1,
                    verseEnd = 1,
                    version = "RVR1960",
                ),
                primaryText = StyledText.plain("1 Jehová es mi pastor; nada me faltará."),
                primaryVersion = "RVR1960",
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Reflexión"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("¿Qué me enseña este versículo sobre Dios?"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Aplicación Personal"),
            ),
            StudyBlock.Reflection(
                prompt = "¿Cómo puedo aplicar esto hoy?",
                text = StyledText.plain("Escribe tu reflexión"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Oración"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Escribe tu oración basada en este versículo"),
            ),
        )

        override fun generateMetadata(): DocMetadata = DocMetadata(
            tags = listOf("devocional", "individual"),
        )
    }

    object BibleStudy : StudyTemplate(
        id = "bible_study",
        name = "Estudio Bíblico",
        description = "Análisis detallado de un tema o pasaje",
        icon = "📚",
    ) {
        override fun generateInitialBlocks(): List<StudyBlock> = listOf(
            StudyBlock.Heading(
                level = 1,
                text = StyledText.plain("Tema del Estudio"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Descripción general del tema"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Pasajes Relacionados"),
            ),
            StudyBlock.BulletList(
                items = listOf(
                    StyledText.plain("Juan 3:16 - El amor de Dios"),
                    StyledText.plain("Romanos 5:8 - La gracia"),
                    StyledText.plain("Efesios 2:8-9 - Salvación por gracia"),
                ),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Contexto Histórico"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Información sobre el contexto del pasaje"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Análisis del Texto"),
            ),
            StudyBlock.Quote(
                text = StyledText.plain("Cita relevante de comentario bíblico"),
                attribution = "Autor del comentario",
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Preguntas de Discusión"),
            ),
            StudyBlock.NumberedList(
                items = listOf(
                    StyledText.plain("¿Qué aprendemos sobre Dios en este pasaje?"),
                    StyledText.plain("¿Cómo se aplica esto a nuestra vida hoy?"),
                    StyledText.plain("¿Qué cambios debemos hacer?"),
                ),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Notas Adicionales"),
            ),
            StudyBlock.Note(
                text = StyledText.plain("Información complementaria"),
            ),
        )

        override fun generateMetadata(): DocMetadata = DocMetadata(
            tags = listOf("estudio-biblico", "grupo"),
        )
    }

    object BibleClass : StudyTemplate(
        id = "bible_class",
        name = "Clase Bíblica",
        description = "Material para enseñanza en grupo",
        icon = "🎓",
    ) {
        override fun generateInitialBlocks(): List<StudyBlock> = listOf(
            StudyBlock.Heading(
                level = 1,
                text = StyledText.plain("Objetivos de Aprendizaje"),
            ),
            StudyBlock.BulletList(
                items = listOf(
                    StyledText.plain("Al final de esta clase, el estudiante podrá..."),
                    StyledText.plain("Comprender el contexto del pasaje"),
                    StyledText.plain("Aplicar los principios a su vida"),
                ),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Introducción"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Rompehielo y conexión con el tema"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Contenido Principal"),
            ),
            StudyBlock.Heading(
                level = 3,
                text = StyledText.plain("Sección 1"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Explicación del primer concepto"),
            ),
            StudyBlock.Heading(
                level = 3,
                text = StyledText.plain("Sección 2"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Explicación del segundo concepto"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Actividad Práctica"),
            ),
            StudyBlock.TodoList(
                items = listOf(
                    StudyBlock.TodoList.TodoItem(
                        text = StyledText.plain("Leer el pasaje en voz alta"),
                        checked = false,
                    ),
                    StudyBlock.TodoList.TodoItem(
                        text = StyledText.plain("Discutir en grupos pequeños"),
                        checked = false,
                    ),
                    StudyBlock.TodoList.TodoItem(
                        text = StyledText.plain("Compartir aplicaciones personales"),
                        checked = false,
                    ),
                ),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Evaluación"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Preguntas para verificar comprensión"),
            ),
            StudyBlock.Heading(
                level = 2,
                text = StyledText.plain("Tarea"),
            ),
            StudyBlock.Paragraph(
                text = StyledText.plain("Asignación para la próxima clase"),
            ),
        )

        override fun generateMetadata(): DocMetadata = DocMetadata(
            tags = listOf("clase", "grupo"),
        )
    }

    object Blank : StudyTemplate(
        id = "blank",
        name = "Documento en Blanco",
        description = "Comenzar desde cero",
        icon = "📄",
    ) {
        override fun generateInitialBlocks(): List<StudyBlock> = listOf(
            StudyBlock.Paragraph(text = StyledText.Empty),
        )

        override fun generateMetadata(): DocMetadata = DocMetadata.Empty
    }

    companion object {
        fun getAll(): List<StudyTemplate> = listOf(
            ExpositorySermon,
            Devotional,
            BibleStudy,
            BibleClass,
            Blank,
        )

        fun getById(id: String): StudyTemplate? = getAll().find { it.id == id }
    }
}
