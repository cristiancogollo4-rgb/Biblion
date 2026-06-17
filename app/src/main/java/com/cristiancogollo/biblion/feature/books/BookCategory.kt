package com.cristiancogollo.biblion.feature.books

import androidx.compose.ui.graphics.Color

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

object BookCategoryColors {
    private val lightColors = mapOf(
        BookCategory.PENTATEUCO to CategoryColorSchema(Color(0xFFEAF3DE), Color(0xFF27500A), Color(0xFF97C459)),
        BookCategory.HISTORICO to CategoryColorSchema(Color(0xFFE6F1FB), Color(0xFF0C447C), Color(0xFF85B7EB)),
        BookCategory.SAPIENCIAL to CategoryColorSchema(Color(0xFFFAEEDA), Color(0xFF633806), Color(0xFFEF9F27)),
        BookCategory.PROFETICO to CategoryColorSchema(Color(0xFFFBEAF0), Color(0xFF72243E), Color(0xFFED93B1)),
        BookCategory.EVANGELIO to CategoryColorSchema(Color(0xFFE1F5EE), Color(0xFF085041), Color(0xFF5DCAA5)),
        BookCategory.HISTORICO_NT to CategoryColorSchema(Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFF7DD3FC)),
        BookCategory.EPISTOLA_PAULINA to CategoryColorSchema(Color(0xFFF5F3FF), Color(0xFF4C1D95), Color(0xFFC7D2FE)),
        BookCategory.EPISTOLA_CATOLICA to CategoryColorSchema(Color(0xFFEEEDFE), Color(0xFF3C3489), Color(0xFFAFA9EC)),
        BookCategory.APOCALIPSIS to CategoryColorSchema(Color(0xFFFFF1F2), Color(0xFF9F1239), Color(0xFFFDA4AF))
    )

    private val darkColors = mapOf(
        BookCategory.PENTATEUCO to CategoryColorSchema(Color(0xFF1E2816), Color(0xFFC3E2A4), Color(0xFF3E5429)),
        BookCategory.HISTORICO to CategoryColorSchema(Color(0xFF132335), Color(0xFFB9D8F7), Color(0xFF274465)),
        BookCategory.SAPIENCIAL to CategoryColorSchema(Color(0xFF2E2214), Color(0xFFF7D9B2), Color(0xFF5A442B)),
        BookCategory.PROFETICO to CategoryColorSchema(Color(0xFF331A23), Color(0xFFF9C6D6), Color(0xFF5E2E3F)),
        BookCategory.EVANGELIO to CategoryColorSchema(Color(0xFF132A24), Color(0xFFB1EBE0), Color(0xFF25574A)),
        BookCategory.HISTORICO_NT to CategoryColorSchema(Color(0xFF0C293A), Color(0xFF7DD3FC), Color(0xFF1B4E6B)),
        BookCategory.EPISTOLA_PAULINA to CategoryColorSchema(Color(0xFF231834), Color(0xFFD8B4FE), Color(0xFF4338CA)),
        BookCategory.EPISTOLA_CATOLICA to CategoryColorSchema(Color(0xFF1F1B3E), Color(0xFFC7C2FA), Color(0xFF3F3B77)),
        BookCategory.APOCALIPSIS to CategoryColorSchema(Color(0xFF3C161D), Color(0xFFFECDD3), Color(0xFF6F1D2F))
    )

    fun getColors(category: BookCategory, isDarkTheme: Boolean): CategoryColorSchema {
        return (if (isDarkTheme) darkColors[category] else lightColors[category])
            ?: CategoryColorSchema(Color.LightGray, Color.DarkGray, Color.Gray)
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
