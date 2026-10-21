package com.krunal.rootd.session

import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.testing.T0
import com.krunal.rootd.testing.aSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class SessionTimingTest {

    private val timer25 = aSession(mode = SessionMode.TIMER, plannedMinutes = 25)

    private fun at(seconds: Long) = T0.plusSeconds(seconds)

    @Test
    fun growthStage_boundaries_24_25_49_50_74_75_percent() {
        // 25 min = 1500 s → 1 % = 15 s
        assertEquals(GrowthStage.SEED, SessionTiming.growthStage(timer25, at(24 * 15)))
        assertEquals(GrowthStage.SPROUT, SessionTiming.growthStage(timer25, at(25 * 15)))
        assertEquals(GrowthStage.SPROUT, SessionTiming.growthStage(timer25, at(49 * 15)))
        assertEquals(GrowthStage.SAPLING, SessionTiming.growthStage(timer25, at(50 * 15)))
        assertEquals(GrowthStage.SAPLING, SessionTiming.growthStage(timer25, at(74 * 15)))
        assertEquals(GrowthStage.TREE, SessionTiming.growthStage(timer25, at(75 * 15)))
    }

    @Test
    fun growthStage_stopwatch_usesThe120MinuteCap() {
        val stopwatch = aSession(mode = SessionMode.STOPWATCH)
        assertEquals(GrowthStage.SEED, SessionTiming.growthStage(stopwatch, at(29 * 60)))
        assertEquals(GrowthStage.SPROUT, SessionTiming.growthStage(stopwatch, at(30 * 60)))
    }

    @Test
    fun timerEnd_stopwatch_isNull() {
        assertNull(SessionTiming.timerEnd(aSession(mode = SessionMode.STOPWATCH), at(60)))
    }

    @Test
    fun timerEnd_addsCompletedAndCurrentPauses() {
        val paused = timer25.copy(pausedSeconds = 120, pauseStartedAt = at(600))
        // 25 min + 2 min finished pause + 1 min current pause (now = 660 s)
        assertEquals(at(25 * 60 + 120 + 60), SessionTiming.timerEnd(paused, at(660)))
    }

    @Test
    fun elapsedFocus_excludesCompletedAndCurrentPauses() {
        val paused = timer25.copy(pausedSeconds = 120, pauseStartedAt = at(600))
        // 660 s wall − 120 s finished pause − 60 s current pause
        assertEquals(Duration.ofSeconds(480), SessionTiming.elapsedFocus(paused, at(660)))
    }

    @Test
    fun elapsedFocus_clockBeforeStart_isZero() {
        assertEquals(Duration.ZERO, SessionTiming.elapsedFocus(timer25, T0.minusSeconds(30)))
    }

    @Test
    fun elapsedFocus_neverExceedsWallTime() {
        val weird = timer25.copy(pausedSeconds = -500) // corrupted row must not inflate focus
        assertEquals(Duration.ofSeconds(100), SessionTiming.elapsedFocus(weird, at(100)))
    }
}
