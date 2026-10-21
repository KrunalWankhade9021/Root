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
    fun cancel_clockMovedBackwards_endedAtNotBeforeStart() = runTest { // Review Focus 2
        val s = engine.start(timer())
        clock.now = T0.minusSeconds(120)
        assertTrue(engine.cancel())
        assertEquals(T0, sessions.get(s.id)!!.endedAt)
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

    // ---- passes ----

    private suspend fun allowPass(sessionId: Long, minutes: Int = 2): Long = passes.insert(
        com.krunal.rootd.testing.aPassRequest(sessionId = sessionId, askedAt = clock.now, grantedMinutes = minutes),
    )

    @Test
    fun beginPass_pausesClock_marksBreak_startsPass() = runTest {
        val s = engine.start(timer())
        advance(5 * 60)
        val p = allowPass(s.id)
        engine.beginPass(p)
        val during = sessions.get(s.id)!!
        assertEquals(clock.now, during.pauseStartedAt)
        assertTrue(during.tookBreak)
        assertEquals(1, during.passCount)
        assertEquals(clock.now, passes.get(p)!!.passStartedAt)
        advance(60)
        assertEquals(Duration.ofMinutes(5), SessionTiming.elapsedFocus(sessions.get(s.id)!!, clock.now))
    }

    @Test
    fun pass_2min_shiftsTimerEndBy2min() = runTest {
        val s = engine.start(timer())
        advance(5 * 60)
        engine.beginPass(allowPass(s.id, minutes = 2))
        advance(2 * 60)
        engine.tick() // pass expires here
        advance(19 * 60 + 59) // 26:59 wall
        assertEquals(SessionStatus.ONGOING, engine.tick()!!.status)
        advance(1) // 27:00 wall
        val done = engine.tick()!!
        assertEquals(SessionStatus.GROWN, done.status)
        assertEquals(T0.plusSeconds(27 * 60), done.endedAt)
        assertEquals(25L * 60, done.focusSeconds)
        assertEquals(120L, done.pausedSeconds)
    }

    @Test
    fun endPass_returnedEarly_recordsUsedTimeAndReturnedBeforeExpiry() = runTest {
        val s = engine.start(timer())
        advance(5 * 60)
        val p = allowPass(s.id, minutes = 3)
        engine.beginPass(p)
        advance(90)
        engine.endPass(com.krunal.rootd.domain.model.PassEndReason.RETURNED_TO_ROOTD)
        val pass = passes.get(p)!!
        assertEquals(com.krunal.rootd.domain.model.PassEndReason.RETURNED_TO_ROOTD, pass.passEndReason)
        assertEquals(90L, pass.usedSeconds)
        assertEquals(true, pass.returnedBeforeExpiry)
        assertEquals(clock.now, pass.passEndedAt)
        val after = sessions.get(s.id)!!
        assertNull(after.pauseStartedAt)
        assertEquals(90L, after.pausedSeconds)
    }

    @Test
    fun endPass_usedSecondsCappedAtGranted() = runTest {
        val s = engine.start(timer(60))
        advance(5 * 60)
        val p = allowPass(s.id, minutes = 2)
        engine.beginPass(p)
        advance(5 * 60) // came back 3 minutes late
        engine.endPass(com.krunal.rootd.domain.model.PassEndReason.RETURNED_TO_ROOTD)
        val pass = passes.get(p)!!
        assertEquals(120L, pass.usedSeconds)
        assertEquals(com.krunal.rootd.domain.model.PassEndReason.EXPIRED, pass.passEndReason)
        assertEquals(false, pass.returnedBeforeExpiry)
        assertEquals(T0.plusSeconds(7 * 60), pass.passEndedAt)
        assertEquals(120L, sessions.get(s.id)!!.pausedSeconds) // focus resumes at expiry, not when they came back
    }

    @Test
    fun endPass_noActivePass_isNoOp() = runTest {
        val s = engine.start(timer())
        engine.endPass(com.krunal.rootd.domain.model.PassEndReason.RETURNED_TO_ROOTD)
        assertEquals(s, sessions.get(s.id))
    }

    @Test
    fun beginPass_guards() = runTest {
        val s = engine.start(timer(60))
        advance(60)
        val denied = passes.insert(com.krunal.rootd.testing.aPassRequest(sessionId = s.id, verdict = com.krunal.rootd.domain.model.Verdict.DENY))
        val other = passes.insert(com.krunal.rootd.testing.aPassRequest(sessionId = s.id + 99))
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.beginPass(denied) } }
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.beginPass(other) } }

        val first = allowPass(s.id)
        engine.beginPass(first)
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.beginPass(allowPass(s.id)) } } // one at a time
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.beginPass(first) } } // already started
        engine.endPass(com.krunal.rootd.domain.model.PassEndReason.RETURNED_TO_ROOTD)
        engine.beginPass(allowPass(s.id))
        engine.endPass(com.krunal.rootd.domain.model.PassEndReason.RETURNED_TO_ROOTD)
        assertThrows(IllegalStateException::class.java) { kotlinx.coroutines.runBlocking { engine.beginPass(allowPass(s.id)) } } // max 2
        assertEquals(2, sessions.get(s.id)!!.passCount)
    }

    @Test
    fun giveUp_duringPass_closesPassSessionEnded_andSettlesOnce() = runTest { // Review Focus 5
        val s = engine.start(timer())
        advance(5 * 60)
        val p = allowPass(s.id)
        engine.beginPass(p)
        advance(60)
        val done = engine.giveUp()
        val pass = passes.get(p)!!
        assertEquals(com.krunal.rootd.domain.model.PassEndReason.SESSION_ENDED, pass.passEndReason)
        assertEquals(60L, pass.usedSeconds)
        assertEquals(false, pass.returnedBeforeExpiry)
        assertEquals(300L, done.focusSeconds)
        assertEquals(60L, done.pausedSeconds)
        assertNull(done.pauseStartedAt)
        assertEquals(listOf(s.id), coins.settled)
        assertNull(passes.activePass(s.id))
    }

    @Test
    fun tick_passExpiredThenTimerDue_closesPassAtExpiryThenGrows() = runTest { // Review Focus 5 (see ledger ruling)
        val s = engine.start(timer())
        advance(24 * 60)
        val p = allowPass(s.id, minutes = 2)
        engine.beginPass(p)
        advance(16 * 60) // nothing ran in between (process dead); now 40:00 wall
        val done = engine.tick()!!
        val pass = passes.get(p)!!
        assertEquals(com.krunal.rootd.domain.model.PassEndReason.EXPIRED, pass.passEndReason)
        assertEquals(T0.plusSeconds(26 * 60), pass.passEndedAt)
        assertEquals(SessionStatus.GROWN, done.status)
        assertEquals(T0.plusSeconds(27 * 60), done.endedAt)
        assertEquals(25L * 60, done.focusSeconds)
        assertEquals(listOf(s.id), coins.settled)
    }

    // ---- recovery (a fresh engine over the same stored data = app restarted) ----

    private fun restartedEngine() = SessionEngine(sessions, passes, coins, clock)

    @Test
    fun recover_afterProcessDeath_passExpiredWhileDead_closedAtExpiry() = runTest { // Review Focus 1
        val s = engine.start(timer(60))
        advance(5 * 60)
        val p = allowPass(s.id, minutes = 3)
        engine.beginPass(p)
        advance(10 * 60)
        val after = restartedEngine().recover()!!
        val pass = passes.get(p)!!
        assertEquals(com.krunal.rootd.domain.model.PassEndReason.EXPIRED, pass.passEndReason)
        assertEquals(T0.plusSeconds(8 * 60), pass.passEndedAt)
        assertEquals(180L, pass.usedSeconds)
        assertEquals(SessionStatus.ONGOING, after.status)
        assertNull(after.pauseStartedAt)
        assertEquals(180L, after.pausedSeconds)
        assertEquals(Duration.ofMinutes(12), SessionTiming.elapsedFocus(after, clock.now))
    }

    @Test
    fun recover_afterProcessDeath_timerAlreadyDue_grownAtPlannedEnd() = runTest { // Review Focus 1
        val s = engine.start(timer())
        advance(2 * 3600)
        val done = restartedEngine().recover()!!
        assertEquals(SessionStatus.GROWN, done.status)
        assertEquals(T0.plusSeconds(25 * 60), done.endedAt)
        assertEquals(listOf(s.id), coins.settled)
    }

    @Test
    fun recover_danglingPauseWithoutActivePass_isClosed() = runTest {
        val s = engine.start(timer(60))
        advance(5 * 60)
        sessions.update(sessions.get(s.id)!!.copy(pauseStartedAt = clock.now)) // pause with no pass (corrupted)
        advance(60)
        val after = restartedEngine().recover()!!
        assertNull(after.pauseStartedAt)
        assertEquals(60L, after.pausedSeconds) // not credited as focus
    }

    @Test
    fun recover_activePassWithoutPause_restoresPauseFromPassStart() = runTest {
        val s = engine.start(timer(60))
        advance(5 * 60)
        val p = allowPass(s.id, minutes = 5)
        engine.beginPass(p)
        sessions.update(sessions.get(s.id)!!.copy(pauseStartedAt = null)) // lost the pause marker (corrupted)
        advance(60)
        val after = restartedEngine().recover()!!
        assertEquals(T0.plusSeconds(5 * 60), after.pauseStartedAt)
        assertEquals(Duration.ofMinutes(5), SessionTiming.elapsedFocus(after, clock.now))
    }

    @Test
    fun elapsed_clockMovedBackwards_isClampedToZero() = runTest { // Review Focus 2
        val s = engine.start(timer())
        clock.now = T0.minusSeconds(3600)
        assertEquals(SessionStatus.ONGOING, engine.tick()!!.status)
        assertEquals(Duration.ZERO, SessionTiming.elapsedFocus(sessions.get(s.id)!!, clock.now))
        val done = engine.giveUp()
        assertEquals(0L, done.focusSeconds)
        assertEquals(T0, done.endedAt) // never before startedAt
    }
}
