package com.krunal.rootd.session

import com.krunal.rootd.domain.model.FailReason
import com.krunal.rootd.domain.model.Session
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.repo.CoinLedger
import com.krunal.rootd.domain.repo.PassRequestRepository
import com.krunal.rootd.domain.repo.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * All session rules (design §4.1, data-model §3): one ongoing session at a time, the 10-second cancel window,
 * give up, stopwatch stop, timer/stopwatch completion, pass pause/resume and restart recovery.
 * Every state change goes through [lock] so racing callers (UI, alarm, accessibility service) can't double-finish.
 */
class SessionEngine(
    private val sessions: SessionRepository,
    private val passes: PassRequestRepository,
    private val coins: CoinLedger,
    private val clock: Clock,
) {
    private val lock = Mutex()

    fun observeOngoing(): Flow<Session?> = sessions.observeOngoing()

    suspend fun start(request: StartRequest): Session = lock.withLock {
        when (request.mode) {
            SessionMode.TIMER -> {
                val m = requireNotNull(request.plannedMinutes) { "Timer sessions need plannedMinutes" }
                require(m in 10..120 && m % 5 == 0) { "Timer minutes must be 10..120 in steps of 5, was $m" }
            }
            SessionMode.STOPWATCH -> require(request.plannedMinutes == null) { "Stopwatch sessions have no plannedMinutes" }
        }
        check(sessions.getOngoing() == null) { "A session is already ongoing" }
        val now = clock.instant()
        val draft = Session(
            mode = request.mode,
            plannedMinutes = request.plannedMinutes,
            startedAt = now,
            startedLocalDate = LocalDate.ofInstant(now, clock.zone),
            speciesId = request.speciesId,
            tagId = request.tagId,
            allowListId = request.allowListId,
            goalNote = request.goalNote,
        )
        val id = sessions.insert(draft)
        draft.copy(id = id)
    }

    suspend fun canCancel(): Boolean {
        val s = sessions.getOngoing() ?: return false
        return withinCancelWindow(s, clock.instant())
    }

    suspend fun cancel(): Boolean = lock.withLock {
        val s = sessions.getOngoing() ?: return@withLock false
        val now = clock.instant()
        if (!withinCancelWindow(s, now)) return@withLock false
        sessions.update(
            s.copy(
                status = SessionStatus.CANCELLED,
                endedAt = now,
                focusSeconds = SessionTiming.elapsedFocus(s, now).seconds,
            ),
        )
        true
    }

    suspend fun giveUp(): Session = lock.withLock {
        val s = checkNotNull(sessions.getOngoing()) { "No ongoing session" }
        finish(s, SessionStatus.WITHERED, FailReason.GAVE_UP, clock.instant())
    }

    suspend fun stopStopwatch(): Session = lock.withLock {
        val s = checkNotNull(sessions.getOngoing()) { "No ongoing session" }
        check(s.mode == SessionMode.STOPWATCH) { "Only stopwatch sessions can be stopped" }
        val now = clock.instant()
        capInstant(s, now)?.let { return@withLock finish(s, SessionStatus.GROWN, null, it) }
        val focusMinutes = SessionTiming.elapsedFocus(s, now).toMinutes()
        if (focusMinutes >= SessionTiming.STOPWATCH_MIN_MINUTES) {
            finish(s, SessionStatus.GROWN, null, now)
        } else {
            finish(s, SessionStatus.WITHERED, FailReason.STOPWATCH_TOO_SHORT, now)
        }
    }

    /**
     * Completes a due timer or a stopwatch that reached its cap. Returns the ongoing session, the session it just
     * finished, or null when there is none. Safe to call any time and from anywhere (UI tick, alarm, app start).
     */
    suspend fun tick(): Session? = lock.withLock {
        val s = sessions.getOngoing() ?: return@withLock null
        val now = clock.instant()
        val due = when (s.mode) {
            SessionMode.TIMER -> SessionTiming.timerEnd(s, now)?.takeIf { !now.isBefore(it) }
            SessionMode.STOPWATCH -> capInstant(s, now)
        }
        if (due != null) finish(s, SessionStatus.GROWN, null, due) else s
    }

    /** The instant a stopwatch reached the 120-minute cap, or null if it hasn't yet. */
    private fun capInstant(s: Session, now: Instant): Instant? {
        val cap = Duration.ofMinutes(SessionTiming.STOPWATCH_CAP_MINUTES.toLong())
        val focus = SessionTiming.elapsedFocus(s, now)
        return if (focus >= cap) now - (focus - cap) else null
    }

    /** The only way a session leaves ONGOING (apart from cancel). Settles coins for grown/withered. */
    private suspend fun finish(s: Session, status: SessionStatus, reason: FailReason?, at: Instant): Session {
        val done = s.copy(
            status = status,
            failReason = reason,
            endedAt = at,
            focusSeconds = SessionTiming.elapsedFocus(s, at).seconds,
        )
        sessions.update(done)
        coins.settleSession(done.id)
        return done
    }

    private fun withinCancelWindow(s: Session, now: Instant): Boolean =
        Duration.between(s.startedAt, now) < Duration.ofSeconds(SessionTiming.CANCEL_WINDOW_SECONDS)
}
