package com.krunal.rootd.session

import com.krunal.rootd.data.memory.InMemoryCatalogRepository
import com.krunal.rootd.data.memory.InMemoryCoinLedger
import com.krunal.rootd.data.memory.InMemoryPassRequestRepository
import com.krunal.rootd.data.memory.InMemorySessionRepository
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.repo.CoinLedger
import com.krunal.rootd.testing.FixedClock
import com.krunal.rootd.testing.T0
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate

/** Counts settleSession calls on top of the real in-memory ledger. */
private class SpyCoinLedger(private val real: CoinLedger) : CoinLedger by real {
    val settled = mutableListOf<Long>()
    override suspend fun settleSession(sessionId: Long) {
        settled += sessionId
        real.settleSession(sessionId)
    }
}

class SessionEngineTest {

    private val clock = FixedClock(now = T0) // 2026-10-21T09:00Z, Asia/Kolkata
    private val sessions = InMemorySessionRepository()
    private val passes = InMemoryPassRequestRepository()
    private val coins = SpyCoinLedger(InMemoryCoinLedger(sessions, passes, InMemoryCatalogRepository(clock), clock))
    private val engine = SessionEngine(sessions, passes, coins, clock)

    private fun timer(minutes: Int = 25) = StartRequest(SessionMode.TIMER, minutes, speciesId = 1, tagId = null, allowListId = null, goalNote = "notes")
    private fun stopwatch() = StartRequest(SessionMode.STOPWATCH, null, speciesId = 1, tagId = null, allowListId = null, goalNote = null)
    private fun advance(seconds: Long) = clock.advance(Duration.ofSeconds(seconds))

    // ---- start ----

    @Test
    fun start_timer25_createsOngoingWithLocalDate() = runTest {
        val s = engine.start(timer())
        assertEquals(SessionStatus.ONGOING, s.status)
        assertEquals(LocalDate.parse("2026-10-21"), s.startedLocalDate)
        assertEquals(T0, s.startedAt)
        assertEquals(25, s.plannedMinutes)
        assertEquals(s, sessions.getOngoing())
    }

    @Test
    fun start_localDateUsesDeviceZone_notUtc() = runTest {
        clock.now = java.time.Instant.parse("2026-10-21T20:00:00Z") // 01:30 on the 22nd in Asia/Kolkata
        assertEquals(LocalDate.parse("2026-10-22"), engine.start(timer()).startedLocalDate)
    }

    @Test
    fun start_whileOngoing_throws() = runTest {
        engine.start(timer())
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.start(timer()) } }
    }

    @Test
    fun start_timerMinutes7_or_33_throwsIllegalArgument() = runTest {
        for (bad in listOf(7, 33, 5, 125)) {
            assertThrows(IllegalArgumentException::class.java) { kotlinx.coroutines.runBlocking { engine.start(timer(bad)) } }
        }
        assertNull(sessions.getOngoing())
    }

    @Test
    fun start_timerMinutes10_and_120_allowed() = runTest {
        engine.start(timer(10))
        engine.cancel()
        engine.start(timer(120))
    }

    @Test
    fun start_stopwatchWithMinutes_throwsIllegalArgument() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { engine.start(stopwatch().copy(plannedMinutes = 25)) }
        }
    }

    // ---- cancel ----

    @Test
    fun cancel_at9s_cancelled_noCoinCall() = runTest {
        val s = engine.start(timer())
        advance(9)
        assertTrue(engine.canCancel())
        assertTrue(engine.cancel())
        val after = sessions.get(s.id)!!
        assertEquals(SessionStatus.CANCELLED, after.status)
        assertEquals(T0.plusSeconds(9), after.endedAt)
        assertTrue(coins.settled.isEmpty())
        assertNull(sessions.getOngoing())
    }

    @Test
    fun cancel_at11s_returnsFalse_staysOngoing() = runTest {
        val s = engine.start(timer())
        advance(11)
        assertFalse(engine.canCancel())
        assertFalse(engine.cancel())
        assertEquals(SessionStatus.ONGOING, sessions.get(s.id)!!.status)
    }

    @Test
    fun cancel_at10s_isOutsideWindow() = runTest {
        engine.start(timer())
        advance(10)
        assertFalse(engine.canCancel())
    }

    @Test
    fun cancel_noSession_returnsFalse() = runTest {
        assertFalse(engine.canCancel())
        assertFalse(engine.cancel())
    }

    // ---- completion ----

    @Test
    fun tick_before25min_staysOngoing() = runTest {
        engine.start(timer())
        advance(25 * 60 - 1)
        assertEquals(SessionStatus.ONGOING, engine.tick()!!.status)
        assertTrue(coins.settled.isEmpty())
    }

    @Test
    fun tick_at25min_timerGrown_endedAtExactlyPlannedEnd() = runTest {
        val s = engine.start(timer())
        advance(25 * 60)
        val done = engine.tick()!!
        assertEquals(SessionStatus.GROWN, done.status)
        assertEquals(T0.plusSeconds(25 * 60), done.endedAt)
        assertEquals(25L * 60, done.focusSeconds)
        assertEquals(listOf(s.id), coins.settled)
        assertNull(sessions.getOngoing())
    }

    @Test
    fun tick_runningLate_endedAtIsPlannedEndNotNow() = runTest {
        engine.start(timer())
        advance(25 * 60 + 3 * 3600)
        val done = engine.tick()!!
        assertEquals(T0.plusSeconds(25 * 60), done.endedAt)
        assertEquals(25L * 60, done.focusSeconds)
    }

    @Test
    fun tick_noSession_returnsNull() = runTest {
        assertNull(engine.tick())
    }

    @Test
    fun stopStopwatch_at9m59s_witheredTooShort() = runTest {
        engine.start(stopwatch())
        advance(9 * 60 + 59)
        val done = engine.stopStopwatch()
        assertEquals(SessionStatus.WITHERED, done.status)
        assertEquals(com.krunal.rootd.domain.model.FailReason.STOPWATCH_TOO_SHORT, done.failReason)
        assertEquals(listOf(done.id), coins.settled)
    }

    @Test
    fun stopStopwatch_at10m_grown() = runTest {
        engine.start(stopwatch())
        advance(10 * 60)
        val done = engine.stopStopwatch()
        assertEquals(SessionStatus.GROWN, done.status)
        assertNull(done.failReason)
        assertEquals(600L, done.focusSeconds)
    }

    @Test
    fun stopStopwatch_onTimerSession_throws() = runTest {
        engine.start(timer())
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.stopStopwatch() } }
    }

    @Test
    fun tick_stopwatchAt120min_grown() = runTest {
        engine.start(stopwatch())
        advance(119 * 60 + 59)
        assertEquals(SessionStatus.ONGOING, engine.tick()!!.status)
        advance(1)
        val done = engine.tick()!!
        assertEquals(SessionStatus.GROWN, done.status)
        assertEquals(7200L, done.focusSeconds)
        assertEquals(T0.plusSeconds(7200), done.endedAt)
    }

    @Test
    fun tick_stopwatchCapRunningLate_endedAtTheCapInstant() = runTest {
        engine.start(stopwatch())
        advance(5 * 3600)
        val done = engine.tick()!!
        assertEquals(T0.plusSeconds(7200), done.endedAt)
        assertEquals(7200L, done.focusSeconds)
    }

    @Test
    fun giveUp_withersWithGaveUp_andSettlesOnce() = runTest {
        val s = engine.start(timer())
        advance(5 * 60)
        val done = engine.giveUp()
        assertEquals(SessionStatus.WITHERED, done.status)
        assertEquals(com.krunal.rootd.domain.model.FailReason.GAVE_UP, done.failReason)
        assertEquals(300L, done.focusSeconds)
        assertEquals(T0.plusSeconds(300), done.endedAt)
        assertEquals(listOf(s.id), coins.settled)
    }

    @Test
    fun giveUp_noSession_throws() = runTest {
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.giveUp() } }
    }

    @Test
    fun finishedSession_neverChangesAgain() = runTest {
        val s = engine.start(timer())
        advance(25 * 60)
        engine.tick()
        advance(60)
        assertNull(engine.tick())
        assertFalse(engine.cancel())
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.giveUp() } }
        assertEquals(SessionStatus.GROWN, sessions.get(s.id)!!.status)
        assertEquals(listOf(s.id), coins.settled)
    }
}
