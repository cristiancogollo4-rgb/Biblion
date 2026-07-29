package com.cristiancogollo.biblion.feature.achievements.domain

object AchievementCatalog {
    val all = listOf(
        achievement(AchievementIds.FIRST_LIGHT, AchievementCategory.READING, "Primera Luz", "Abre el versículo del día y llega a su capítulo.", "🌅", 1),
        achievement(AchievementIds.BOTH_WORLDS, AchievementCategory.READING, "Ambos Mundos", "Lee un capítulo del Antiguo y otro del Nuevo Testamento.", "🏛️", 2, AchievementTier.PROGRESS),
        achievement(AchievementIds.TWENTY_CHAPTERS, AchievementCategory.READING, "Devorador de Capítulos", "Lee 20 capítulos bíblicos diferentes.", "📖", 20, AchievementTier.MASTERY),
        achievement(AchievementIds.FOUR_COLORS, AchievementCategory.READING, "Coleccionista Arcoíris", "Utiliza los cuatro colores de resaltado.", "🎨", 4, AchievementTier.PROGRESS),
        achievement(AchievementIds.FIRST_SEARCH, AchievementCategory.SEARCH, "Buscador", "Completa tu primera búsqueda de versículos.", "🔎", 1),
        achievement(AchievementIds.OLD_TESTAMENT_SEARCH, AchievementCategory.SEARCH, "Sabiduría Antigua", "Encuentra resultados en el Antiguo Testamento.", "📜", 1),
        achievement(AchievementIds.NEW_TESTAMENT_SEARCH, AchievementCategory.SEARCH, "Buscador del Evangelio", "Encuentra resultados en el Nuevo Testamento.", "✝️", 1),
        achievement(AchievementIds.TOPIC_CATEGORY, AchievementCategory.SEARCH, "Explorador de Categorías", "Abre una categoría en Explorar temas.", "🗂️", 1),
        achievement(AchievementIds.CONNECTED, AchievementCategory.TOPICS, "Conectado", "Abre un pasaje desde una relación temática.", "🔗", 1),
        achievement(AchievementIds.FIRST_TOPIC, AchievementCategory.TOPICS, "Primer Descubrimiento", "Expande tu primera tarjeta temática.", "🧭", 1),
        achievement(AchievementIds.TEN_TOPICS, AchievementCategory.TOPICS, "Conocedor de Temas", "Explora 10 temas diferentes.", "📊", 10, AchievementTier.PROGRESS),
        achievement(AchievementIds.TWENTY_FIVE_TOPICS, AchievementCategory.TOPICS, "Enciclopedia de Temas", "Explora 25 temas diferentes.", "🧠", 25, AchievementTier.MASTERY),
        achievement(AchievementIds.BIBI_WHO, AchievementCategory.BIBI, "Buscador de Personajes", "Consulta a Bibi sobre un personaje bíblico.", "👤", 1),
        achievement(AchievementIds.BIBI_WHERE, AchievementCategory.BIBI, "Rastreador de Lugares", "Consulta a Bibi sobre un lugar bíblico.", "📍", 1),
        achievement(AchievementIds.BIBI_DEFINE, AchievementCategory.BIBI, "Maestro de Palabras", "Pide a Bibi una definición bíblica.", "📖", 1),
        achievement(AchievementIds.BIBI_DIVE_DEEPER, AchievementCategory.BIBI, "Buceador Profundo", "Profundiza una respuesta con Bibi.", "🤿", 1, AchievementTier.PROGRESS),
        achievement(AchievementIds.FIRST_LOCAL_STUDY, AchievementCategory.STUDY, "Primer Fruto", "Guarda localmente tu primera enseñanza con contenido.", "🌿", 1),
        achievement(AchievementIds.SIX_BLOCK_TYPES, AchievementCategory.STUDY, "Constructor Maestro", "Utiliza los seis tipos de bloque en un documento.", "📐", 6, AchievementTier.MASTERY),
        achievement(AchievementIds.VERSE_INSERTED, AchievementCategory.STUDY, "Tejedor de Escrituras", "Inserta un versículo en una enseñanza.", "🔗", 1),
        achievement(AchievementIds.VERSION_COMPARED, AchievementCategory.STUDY, "Comparador de Versiones", "Compara un pasaje en dos versiones.", "🔄", 1, AchievementTier.PROGRESS),
        achievement(AchievementIds.DICTIONARY_PERSON, AchievementCategory.GROWTH, "Explorador de Personas", "Abre una persona del diccionario bíblico.", "👥", 1),
        achievement(AchievementIds.DICTIONARY_PLACE, AchievementCategory.GROWTH, "Turista de Tierra Santa", "Abre un lugar del diccionario bíblico.", "🗺️", 1),
        achievement(AchievementIds.IDENTITY_COMPLETE, AchievementCategory.GROWTH, "Identidad Completa", "Guarda nombre, apellido y alias.", "👤", 3, AchievementTier.PROGRESS),
        achievement(AchievementIds.THREE_DAY_STREAK, AchievementCategory.GROWTH, "Peregrino Constante", "Usa Biblion durante tres días consecutivos.", "📅", 3, AchievementTier.CONSISTENCY),
    )

    val byId = all.associateBy(AchievementDefinition::id)

    private fun achievement(
        id: String,
        category: AchievementCategory,
        title: String,
        description: String,
        icon: String,
        target: Int,
        tier: AchievementTier = AchievementTier.DISCOVERY,
    ) = AchievementDefinition(id, category, title, description, icon, target, tier)
}
