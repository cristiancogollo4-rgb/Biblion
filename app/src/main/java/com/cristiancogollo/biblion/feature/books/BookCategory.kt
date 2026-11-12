package com.cristiancogollo.biblion.feature.books

import androidx.compose.ui.graphics.Color
import com.cristiancogollo.biblion.ui.theme.BiblionHue
import com.cristiancogollo.biblion.ui.theme.BiblionPalette
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode

enum class BookCategory(val labelEs: String, val description: String, val bookCount: Int) {
    PENTATEUCO("Pentateuco", "Orígenes del mundo y del pueblo de Israel", 5),
    HISTORICO("Históricos", "Conquista, reinos y exilio del pueblo escogido", 12),
    SAPIENCIAL("Sapienciales", "Libros de sabiduría, oración y poesía", 5),
    PROFETICO("Proféticos", "Mensajes de advertencia, esperanza y redención", 17),
    EVANGELIO("Evangelios", "Vida, muerte y resurrección del Señor Jesús", 4),
    HISTORICO_NT("Histórico NT", "Nacimiento y expansión de la Iglesia primitiva", 1),
    EPISTOLA_PAULINA("Epístolas Paulinas", "Cartas doctrinales e instructivas del apóstol Pablo", 14),
    EPISTOLA_CATOLICA("Epístolas Católicas", "Cartas generales escritas por otros apóstoles", 7),
    APOCALIPSIS("Profecía", "Revelación de los últimos tiempos y victoria final", 1)
}

data class CategoryColorSchema(val bg: Color, val text: Color, val border: Color)

/**
 * Un matiz sobrio por categoria, derivado de [BiblionPalette] para que las tarjetas se
 * integren con azul, dorado y blanco en los tres temas.
 */
object BookCategoryColors {
    private val hues = mapOf(
        BookCategory.PENTATEUCO to BiblionHue.SAGE,
        BookCategory.HISTORICO to BiblionHue.SLATE,
        BookCategory.SAPIENCIAL to BiblionHue.SAND,
        BookCategory.PROFETICO to BiblionHue.WINE,
        BookCategory.EVANGELIO to BiblionHue.TEAL,
        BookCategory.HISTORICO_NT to BiblionHue.SKY,
        BookCategory.EPISTOLA_PAULINA to BiblionHue.LAVENDER,
        BookCategory.EPISTOLA_CATOLICA to BiblionHue.INDIGO,
        BookCategory.APOCALIPSIS to BiblionHue.TERRACOTTA,
    )

    fun hue(category: BookCategory): BiblionHue = hues.getValue(category)

    fun getColors(category: BookCategory, mode: BiblionThemeMode): CategoryColorSchema {
        val hue = hue(category)
        return CategoryColorSchema(
            bg = BiblionPalette.tint(hue, mode),
            text = BiblionPalette.onTint(hue, mode),
            border = BiblionPalette.border(hue, mode),
        )
    }
}

fun String.toBookCategory(): BookCategory {
    return when (this) {
        // Pentateuco
        "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio" -> BookCategory.PENTATEUCO
        
        // Históricos AT
        "Josue", "Jueces", "Rut", "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", 
        "1 Cronicas", "2 Cronicas", "Esdras", "Nehemias", "Ester" -> BookCategory.HISTORICO
        
        // Sapienciales / Poéticos
        "Job", "Salmos", "Proverbios", "Eclesiastes", "Cantares" -> BookCategory.SAPIENCIAL
        
        // Proféticos
        "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", 
        "Amos", "Abdias", "Jonas", "Miqueas", "Nahum", "Habacuc", "Sofonias", 
        "Hageo", "Zacarias", "Malaquias" -> BookCategory.PROFETICO
        
        // Evangelios
        "Mateo", "Marcos", "Lucas", "Juan" -> BookCategory.EVANGELIO
        
        // Histórico NT
        "Hechos" -> BookCategory.HISTORICO_NT
        
        // Epístolas Paulinas
        "Romanos", "1 Corintios", "2 Corintios", "Galatas", "Efesios", "Filipenses", 
        "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses", "1 Timoteo", "2 Timoteo", 
        "Tito", "Filemon", "Hebreos" -> BookCategory.EPISTOLA_PAULINA
        
        // Epístolas Católicas
        "Santiago", "1 Pedro", "2 Pedro", "1 Juan", "2 Juan", "3 Juan", "Judas" -> BookCategory.EPISTOLA_CATOLICA
        
        // Apocalipsis
        "Apocalipsis" -> BookCategory.APOCALIPSIS
        
        else -> BookCategory.EVANGELIO // Fallback
    }
}
