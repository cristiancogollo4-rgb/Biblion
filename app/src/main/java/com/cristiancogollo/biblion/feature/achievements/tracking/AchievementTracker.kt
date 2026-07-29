package com.cristiancogollo.biblion.feature.achievements.tracking

import android.content.Context
import androidx.room.withTransaction
import com.cristiancogollo.biblion.feature.achievements.data.AchievementDatabase
import com.cristiancogollo.biblion.feature.achievements.data.AchievementProgressEntity
import com.cristiancogollo.biblion.feature.achievements.data.AchievementUniqueValueEntity
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementCatalog
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementDefinition
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementIds
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface AchievementClock {
    fun nowMillis(): Long
    fun currentLocalDate(): LocalDate
}

object SystemAchievementClock : AchievementClock {
    override fun nowMillis() = System.currentTimeMillis()
    override fun currentLocalDate(): LocalDate = LocalDate.now()
}

object AchievementTracker {
    private val mutex = Mutex()
    private val _unlockedEvents = MutableSharedFlow<AchievementDefinition>(extraBufferCapacity = 16)
    val unlockedEvents = _unlockedEvents.asSharedFlow()

    suspend fun track(
        context: Context,
        event: AchievementEvent,
        clock: AchievementClock = SystemAchievementClock,
        notify: Boolean = true,
        countAsMeaningfulUse: Boolean = true,
    ): List<AchievementDefinition> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val database = AchievementDatabase.getInstance(context)
            val unlocked = mutableListOf<AchievementDefinition>()
            database.withTransaction {
                val dao = database.achievementDao()
                val now = clock.nowMillis()
                dao.insertProgress(
                    AchievementCatalog.all.map { definition ->
                        AchievementProgressEntity(
                            achievementId = definition.id,
                            currentValue = 0,
                            targetValue = definition.target,
                            isUnlocked = false,
                            unlockedAt = null,
                            notificationSeen = false,
                            updatedAt = now,
                        )
                    }
                )

                suspend fun setProgress(id: String, value: Int) {
                    val definition = AchievementCatalog.byId.getValue(id)
                    val previous = dao.getProgress(id) ?: return
                    val bounded = value.coerceAtLeast(previous.currentValue)
                    val shouldUnlock = bounded >= definition.target
                    val newlyUnlocked = shouldUnlock && !previous.isUnlocked
                    dao.updateProgress(
                        id = id,
                        value = bounded.coerceAtMost(definition.target),
                        unlocked = previous.isUnlocked || shouldUnlock,
                        unlockedAt = previous.unlockedAt ?: now.takeIf { shouldUnlock },
                        newlyUnlocked = newlyUnlocked,
                        updatedAt = now,
                    )
                    if (newlyUnlocked) unlocked += definition
                }

                suspend fun record(namespace: String, value: String): Int {
                    dao.insertEvidence(
                        AchievementUniqueValueEntity(namespace, value, now)
                    )
                    return dao.evidenceCount(namespace)
                }

                when (event) {
                    is AchievementEvent.DailyVerseOpened ->
                        setProgress(AchievementIds.FIRST_LIGHT, 1)

                    is AchievementEvent.ChapterRead -> {
                        val chapters = record(
                            "reading.chapters",
                            "${event.book.trim().lowercase()}:${event.chapter}",
                        )
                        val testaments = record(
                            "reading.testaments",
                            event.testament.trim().uppercase(),
                        )
                        setProgress(AchievementIds.TWENTY_CHAPTERS, chapters)
                        setProgress(AchievementIds.BOTH_WORLDS, testaments)
                    }

                    is AchievementEvent.HighlightCreated -> {
                        record("reading.highlights", event.verseKey)
                        val colors = record("reading.highlight_colors", event.colorKey)
                        setProgress(AchievementIds.FOUR_COLORS, colors)
                    }

                    is AchievementEvent.SearchCompleted -> if (
                        !event.isTutorial && event.resultCount > 0
                    ) {
                        setProgress(AchievementIds.FIRST_SEARCH, 1)
                        when (event.testament.uppercase()) {
                            "OLD" -> setProgress(AchievementIds.OLD_TESTAMENT_SEARCH, 1)
                            "NEW" -> setProgress(AchievementIds.NEW_TESTAMENT_SEARCH, 1)
                        }
                    }

                    is AchievementEvent.TopicCategoryOpened ->
                        setProgress(AchievementIds.TOPIC_CATEGORY, 1)

                    is AchievementEvent.TopicExpanded -> {
                        val count = record("topics.expanded", event.slug)
                        setProgress(AchievementIds.FIRST_TOPIC, count)
                        setProgress(AchievementIds.TEN_TOPICS, count)
                        setProgress(AchievementIds.TWENTY_FIVE_TOPICS, count)
                    }

                    is AchievementEvent.TopicReferenceOpened ->
                        setProgress(AchievementIds.CONNECTED, 1)

                    is AchievementEvent.BibiAnswerCompleted -> if (
                        !event.isTutorial && event.wasSuccessful
                    ) {
                        when (event.intent.uppercase()) {
                            "WHO" -> setProgress(AchievementIds.BIBI_WHO, 1)
                            "WHERE" -> setProgress(AchievementIds.BIBI_WHERE, 1)
                            "DEFINE" -> setProgress(AchievementIds.BIBI_DEFINE, 1)
                            "DIVE_DEEPER" -> setProgress(AchievementIds.BIBI_DIVE_DEEPER, 1)
                        }
                    }

                    is AchievementEvent.StudySavedLocally -> if (
                        event.isExplicitSave && event.hasTitle && event.hasContent
                    ) {
                        setProgress(AchievementIds.FIRST_LOCAL_STUDY, 1)
                    }

                    is AchievementEvent.StudyBlockTypesChanged ->
                        setProgress(AchievementIds.SIX_BLOCK_TYPES, event.blockTypes.size)

                    is AchievementEvent.VerseBlockInserted ->
                        setProgress(AchievementIds.VERSE_INSERTED, 1)

                    is AchievementEvent.VerseVersionCompared -> if (event.versionCount >= 2) {
                        setProgress(AchievementIds.VERSION_COMPARED, 1)
                    }

                    is AchievementEvent.DictionaryEntryOpened -> when (event.category.uppercase()) {
                        "PERSON" -> setProgress(AchievementIds.DICTIONARY_PERSON, 1)
                        "PLACE" -> setProgress(AchievementIds.DICTIONARY_PLACE, 1)
                    }

                    is AchievementEvent.ProfileSaved -> {
                        val completed = listOf(
                            event.hasName,
                            event.hasLastName,
                            event.hasAlias,
                        ).count { it }
                        setProgress(AchievementIds.IDENTITY_COMPLETE, completed)
                    }

                    is AchievementEvent.MeaningfulUse -> Unit
                }

                val isSuccessfulMeaningfulUse = when (event) {
                    is AchievementEvent.SearchCompleted ->
                        !event.isTutorial && event.resultCount > 0
                    is AchievementEvent.BibiAnswerCompleted ->
                        !event.isTutorial && event.wasSuccessful
                    is AchievementEvent.StudySavedLocally ->
                        event.isExplicitSave && event.hasTitle && event.hasContent
                    else -> true
                }
                if (countAsMeaningfulUse && isSuccessfulMeaningfulUse) {
                    val meaningfulDate = when (event) {
                        is AchievementEvent.MeaningfulUse ->
                            event.localDate?.let(LocalDate::parse) ?: clock.currentLocalDate()
                        else -> clock.currentLocalDate()
                    }
                    record("usage.active_dates", meaningfulDate.toString())
                    val dates = dao.evidenceValues("usage.active_dates")
                        .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
                        .sorted()
                    var currentStreak = 0
                    var longestStreak = 0
                    var previousDate: LocalDate? = null
                    dates.forEach { date ->
                        currentStreak = if (previousDate?.plusDays(1) == date) currentStreak + 1 else 1
                        longestStreak = maxOf(longestStreak, currentStreak)
                        previousDate = date
                    }
                    setProgress(AchievementIds.THREE_DAY_STREAK, longestStreak)
                }
                if (!notify) {
                    unlocked.forEach { dao.markNotificationSeen(it.id) }
                }
            }
            if (notify) unlocked.forEach { _unlockedEvents.tryEmit(it) }
            unlocked
        }
    }
}
