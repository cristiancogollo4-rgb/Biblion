package com.cristiancogollo.biblion.feature.achievements

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.achievements.data.AchievementDatabase
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementCatalog
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementIds
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementClock
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementTracker
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AchievementTrackerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dao by lazy { AchievementDatabase.getInstance(context).achievementDao() }

    @Before
    fun reset() = runBlocking {
        dao.clearEvidence()
        dao.clearProgress()
    }

    @Test
    fun `catalog contains 24 unique production achievements`() {
        assertEquals(24, AchievementCatalog.all.size)
        assertEquals(24, AchievementCatalog.all.map { it.id }.toSet().size)
    }

    @Test
    fun `topic evidence is idempotent`() = runBlocking {
        repeat(4) {
            AchievementTracker.track(context, AchievementEvent.TopicExpanded("grace"))
        }

        assertEquals(1, dao.getProgress(AchievementIds.TEN_TOPICS)?.currentValue)
        assertTrue(dao.getProgress(AchievementIds.FIRST_TOPIC)?.isUnlocked == true)
    }

    @Test
    fun `first fruit requires explicit successful local content save`() = runBlocking {
        AchievementTracker.track(
            context,
            AchievementEvent.StudySavedLocally("draft", true, true, isExplicitSave = false),
        )
        assertFalse(dao.getProgress(AchievementIds.FIRST_LOCAL_STUDY)?.isUnlocked == true)

        AchievementTracker.track(
            context,
            AchievementEvent.StudySavedLocally("empty", true, false, isExplicitSave = true),
        )
        assertFalse(dao.getProgress(AchievementIds.FIRST_LOCAL_STUDY)?.isUnlocked == true)

        AchievementTracker.track(
            context,
            AchievementEvent.StudySavedLocally("saved", true, true, isExplicitSave = true),
        )
        assertTrue(dao.getProgress(AchievementIds.FIRST_LOCAL_STUDY)?.isUnlocked == true)
    }

    @Test
    fun `three distinct consecutive local dates unlock streak`() = runBlocking {
        listOf(
            LocalDate.of(2026, 7, 27),
            LocalDate.of(2026, 7, 28),
            LocalDate.of(2026, 7, 29),
        ).forEach { date ->
            AchievementTracker.track(
                context,
                AchievementEvent.MeaningfulUse(date.toString()),
                clock = FixedClock(date),
            )
        }

        assertTrue(dao.getProgress(AchievementIds.THREE_DAY_STREAK)?.isUnlocked == true)
    }

    private data class FixedClock(private val date: LocalDate) : AchievementClock {
        override fun nowMillis(): Long = date.toEpochDay() * 86_400_000
        override fun currentLocalDate(): LocalDate = date
    }
}
