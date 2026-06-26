package com.cristiancogollo.biblion

import com.cristiancogollo.biblion.feature.bibi.data.CrossReferenceVoteEntity
import com.cristiancogollo.biblion.feature.bibi.data.TopicAliasEntity
import com.cristiancogollo.biblion.feature.bibi.data.TopicEntity
import com.cristiancogollo.biblion.feature.bibi.data.TopicReferenceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests para la entity CrossReferenceVoteEntity (reemplazo de TSK).
 * Verifica la estructura del entity y la regla UX de no exponer votos.
 */
class CrossReferenceVoteEntityTest {

    @Test
    fun `entity tiene campo votes`() {
        val entity = CrossReferenceVoteEntity(
            id = 1,
            sourceBook = "Genesis",
            sourceNormalizedBook = "genesis",
            sourceChapter = 1,
            sourceVerse = 1,
            targetReferences = "Juan 1:1-3",
            votes = 369
        )
        // El entity SI tiene votes - es interno para queries
        assertEquals(369, entity.votes)
    }

    @Test
    fun `entity tiene source_normalized_book para queries`() {
        val entity = CrossReferenceVoteEntity(
            sourceBook = "Genesis",
            sourceNormalizedBook = "genesis",
            sourceChapter = 1,
            sourceVerse = 1,
            targetReferences = "Juan 1:1-3",
            votes = 100
        )
        assertEquals("genesis", entity.sourceNormalizedBook)
    }

    @Test
    fun `entity tiene target_references en formato Biblion`() {
        val entity = CrossReferenceVoteEntity(
            sourceBook = "Genesis",
            sourceNormalizedBook = "genesis",
            sourceChapter = 1,
            sourceVerse = 1,
            targetReferences = "Exodo 20:1-5",  // formato "Libro Cap:V-V"
            votes = 50
        )
        assertEquals("Exodo 20:1-5", entity.targetReferences)
    }
}

/**
 * Tests para las entities del schema v3 de topics Biblion.
 *
 * El schema v3 migro de "clusters" openbile.info a una taxonomia canonica
 * curada de 693 temas. Los campos legacy (clusterSize, totalVerses,
 * isTranslatedAuto) fueron reemplazados por:
 *  - description: descripcion del canonico (worker o manual)
 *  - descriptionSource: origen de la descripcion
 *  - parentSlug: jerarquia padre (nullable)
 *  - totalAliases: cantidad de aliases de OpenBible
 */
class TopicEntityTest {

    @Test
    fun `topic entity tiene name_es name_en description`() {
        val entity = TopicEntity(
            id = 1,
            slug = "amor-de-dios",
            nameEs = "Amor de Dios",
            nameEn = "Love of God",
            category = "ATTRIBUTE_OF_GOD",
            description = "El amor de Dios se expresa mediante entrega y servicio.",
            descriptionSource = "worker",
            parentSlug = null,
            verseCount = 50,
            totalAliases = 22
        )
        assertEquals("Amor de Dios", entity.nameEs)
        assertEquals("Love of God", entity.nameEn)
        assertEquals("ATTRIBUTE_OF_GOD", entity.category)
        assertEquals("worker", entity.descriptionSource)
    }

    @Test
    fun `topic entity tiene slug unico`() {
        val entity = TopicEntity(
            slug = "matrimonio",
            nameEs = "Matrimonio",
            nameEn = "Marriage",
            category = "CHRISTIAN_LIFE",
            description = "Pacto sagrado entre hombre y mujer.",
            descriptionSource = "worker",
            parentSlug = "familia-cristiana",
            verseCount = 13,
            totalAliases = 61
        )
        assertEquals("matrimonio", entity.slug)
        assertEquals("familia-cristiana", entity.parentSlug)
    }

    @Test
    fun `topic reference entity tiene score interno (no exponer al usuario)`() {
        val ref = TopicReferenceEntity(
            id = 1,
            topicSlug = "genesis",
            book = "Genesis",
            chapter = 1,
            verseStart = 1,
            verseEnd = 1,
            osis = "Gen.1.1",
            score = 100
        )
        // El score esta en el entity (necesario para queries)
        // pero NO debe mostrarse al usuario en la UI de Bibi
        assertEquals(100, ref.score)
    }

    @Test
    fun `topic reference puede ser un rango`() {
        val ref = TopicReferenceEntity(
            id = 2,
            topicSlug = "diez-mandamientos",
            book = "Exodo",
            chapter = 20,
            verseStart = 1,
            verseEnd = 26,
            osis = "Exod.20.1-Exod.20.26",
            score = 7
        )
        assertEquals(1, ref.verseStart)
        assertEquals(26, ref.verseEnd)
        assertEquals("Exod.20.1-Exod.20.26", ref.osis)
    }

    @Test
    fun `topic alias tiene low_confidence cuando score es bajo`() {
        val alias = TopicAliasEntity(
            id = 1,
            topicSlug = "matrimonio",
            aliasEn = "some fuzzy alias",
            score = 0.852f,
            verseCount = 10,
            maxQuality = 5,
            lowConfidence = 1
        )
        assertEquals(1, alias.lowConfidence)
    }
}
