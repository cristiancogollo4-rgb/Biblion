package com.cristiancogollo.biblion.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousChapterScrollTest {

    @Test
    fun forwardScrollStopsWhenTheNextChapterReachesItsLine() {
        // El siguiente capitulo empieza 100 px por debajo de su linea.
        val room = forwardRoomBeforeBoundary(nextStartOffset = 700, boundaryLine = 600f)
        val (allowed, excess) = splitScrollAtBoundary(-250f, forwardRoom = room, backwardRoom = Float.POSITIVE_INFINITY)
        assertEquals(-100f, allowed, 0.001f)
        assertEquals(-150f, excess, 0.001f)
    }

    @Test
    fun backwardScrollStopsWhenTheCurrentChapterReachesItsLine() {
        val room = backwardRoomBeforeBoundary(currentStartOffset = 300, boundaryLine = 380f)
        val (allowed, excess) = splitScrollAtBoundary(200f, forwardRoom = Float.POSITIVE_INFINITY, backwardRoom = room)
        assertEquals(80f, allowed, 0.001f)
        assertEquals(120f, excess, 0.001f)
    }

    @Test
    fun scrollingInsideTheChapterIsFree() {
        val (allowed, excess) = splitScrollAtBoundary(
            -50f,
            forwardRoom = forwardRoomBeforeBoundary(null, 600f),
            backwardRoom = backwardRoomBeforeBoundary(null, 380f),
        )
        assertEquals(-50f, allowed, 0f)
        assertEquals(0f, excess, 0f)
    }

    @Test
    fun boundaryAlreadyReachedLeavesNoRoom() {
        assertEquals(0f, forwardRoomBeforeBoundary(nextStartOffset = 500, boundaryLine = 600f), 0f)
        assertEquals(0f, backwardRoomBeforeBoundary(currentStartOffset = 500, boundaryLine = 380f), 0f)
    }

    @Test
    fun shortPushSnapsBackButLongPushOrFlingCommits() {
        assertNull(chapterCommitDirection(30f, velocityY = 0f, distanceThresholdPx = 100f, velocityThresholdPx = 1800f, minPullPx = 20f))
        assertEquals(1, chapterCommitDirection(120f, 0f, 100f, 1800f, 20f))
        // Lanzar hacia arriba (velocidad negativa) en el limite tambien avanza.
        assertEquals(1, chapterCommitDirection(30f, -2500f, 100f, 1800f, 20f))
        // Lanzar en sentido contrario al empuje no confirma.
        assertNull(chapterCommitDirection(30f, 2500f, 100f, 1800f, 20f))
        assertEquals(-1, chapterCommitDirection(-40f, 2500f, 100f, 1800f, 20f))
        assertNull(chapterCommitDirection(0f, -9000f, 100f, 1800f, 20f))
    }

    @Test
    fun windowShiftKeepsTheChapterThatWasLeft() {
        val window = ContinuousChapterWindow(chapter = 7, current = "7", previous = "6", next = "8")
        val forward = window.shift(1)!!
        assertEquals(8, forward.chapter)
        assertEquals("7", forward.previous)
        assertNull(forward.next)
        val backward = window.shift(-1)!!
        assertEquals(6, backward.chapter)
        assertEquals("7", backward.next)
        assertNull(window.copy(next = null).shift(1))
    }

    @Test
    fun bodyStartsAfterEveryChapterStart() {
        assertEquals(0, currentChapterBodyStartIndex(false, true, 30))
        assertEquals(1, currentChapterBodyStartIndex(true, false, 0))
        assertEquals(32, currentChapterBodyStartIndex(true, true, 30))
    }

    @Test
    fun interruptedForwardStepCommitsOnceTheNextChapterPassedItsLine() {
        assertTrue(crossed(direction = 1, startOffset = 400))
        assertFalse(crossed(direction = 1, startOffset = 600))
    }

    @Test
    fun interruptedForwardStepCommitsWhenTheNextChapterLeftTheTopOfTheScreen() {
        assertTrue(crossed(direction = 1, startOffset = null, startIndex = 40, firstVisible = 45, lastVisible = 52))
        assertFalse(crossed(direction = 1, startOffset = null, startIndex = 60, firstVisible = 45, lastVisible = 52))
    }

    @Test
    fun interruptedBackwardStepCommitsOnceTheCurrentChapterDroppedBelowItsLine() {
        assertTrue(crossed(direction = -1, startOffset = 500))
        assertFalse(crossed(direction = -1, startOffset = 380))
        assertTrue(crossed(direction = -1, startOffset = null, startIndex = 60, firstVisible = 45, lastVisible = 52))
    }

    @Test
    fun interruptedStepWithoutKnownStartDoesNotCommit() {
        assertFalse(crossed(direction = 1, startOffset = null, startIndex = null))
    }

    private fun crossed(
        direction: Int,
        startOffset: Int?,
        startIndex: Int? = 10,
        firstVisible: Int = 5,
        lastVisible: Int = 15,
    ) = hasCrossedChapterBoundary(
        direction = direction,
        startOffset = startOffset,
        startIndex = startIndex,
        firstVisibleIndex = firstVisible,
        lastVisibleIndex = lastVisible,
        forwardLine = 600f,
        backwardLine = 380f,
    )
}
