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
}
