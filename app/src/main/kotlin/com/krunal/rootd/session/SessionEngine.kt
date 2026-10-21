package com.krunal.rootd.session

import com.krunal.rootd.domain.model.FailReason
import com.krunal.rootd.domain.model.PassEndReason
import com.krunal.rootd.domain.model.PassRequest
import com.krunal.rootd.domain.model.Session
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.repo.CoinLedger
import com.krunal.rootd.domain.repo.PassRequestRepository
import com.krunal.rootd.domain.repo.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import java.time.Instant

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

    private companion object {
        const val MAX_PASSES = 2
        const val RECOVERY_SETTLE_DAYS = 30L
    }

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
            startedLocalDate = now.atZone(clock.zone).toLocalDate(), // LocalDate.ofInstant needs API 34; minSdk is 29
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
        // A pass can start inside the 10 s window (fallback / fast LLM); cancelling must close it too.
        val closed = passes.activePass(s.id)?.let { closePass(s, it, PassEndReason.SESSION_ENDED, now) } ?: s
        sessions.update(
            closed.copy(
                status = SessionStatus.CANCELLED,
                endedAt = maxOf(now, s.startedAt),
                focusSeconds = SessionTiming.elapsedFocus(closed, now).seconds,
            ),
        )
        true
    }

    suspend fun giveUp(): Session = lock.withLock {
        val now = clock.instant()
        val s = catchUp(checkNotNull(sessions.getOngoing()) { "No ongoing session" }, now)
        if (s.status != SessionStatus.ONGOING) return@withLock s // the timer finished first: it grew
        finish(s, SessionStatus.WITHERED, FailReason.GAVE_UP, now)
    }

    suspend fun stopStopwatch(): Session = lock.withLock {
        check(checkNotNull(sessions.getOngoing()) { "No ongoing session" }.mode == SessionMode.STOPWATCH) {
            "Only stopwatch sessions can be stopped"
        }
        val now = clock.instant()
        val s = catchUp(sessions.getOngoing()!!, now)
        if (s.status != SessionStatus.ONGOING) return@withLock s // reached the 120-minute cap first
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
    suspend fun tick(): Session? = lock.withLock { tickLocked() }

    /**
     * Run once on process start: repairs a pause/pass mismatch left by a crash, then behaves like [tick]
     * (closes a pass that expired while the app was dead, completes a timer that is already due).
     */
    suspend fun recover(): Session? = lock.withLock {
        settleRecentlyFinished()
        val s = sessions.getOngoing() ?: return@withLock null
        val rows = passes.forSession(s.id)
        val active = rows.firstOrNull { it.passStartedAt != null && it.passEndedAt == null }
        val started = rows.count { it.passStartedAt != null }
        var repaired = s.copy(passCount = started, tookBreak = started > 0) // counters follow the pass rows
        if (active == null && repaired.pauseStartedAt != null) {
            // Crashed after closing a pass but before resuming: the pause ended when that pass ended.
            val pauseStart = repaired.pauseStartedAt!!
            val pauseEnd = rows.mapNotNull { it.passEndedAt }.filter { !it.isBefore(pauseStart) }.maxOrNull() ?: pauseStart
            repaired = repaired.copy(
                pausedSeconds = repaired.pausedSeconds + nonNegativeSeconds(pauseStart, pauseEnd),
                pauseStartedAt = null,
            )
        } else if (active != null && repaired.pauseStartedAt == null) {
            repaired = repaired.copy(pauseStartedAt = active.passStartedAt) // crashed between the two writes
        }
        if (repaired != s) sessions.update(repaired)
        tickLocked()
    }

    /** A crash between `finish`'s session write and its coin write leaves a finished, unsettled session. */
    private suspend fun settleRecentlyFinished() {
        val today = clock.instant().atZone(clock.zone).toLocalDate()
        sessions.observeBetween(today.minusDays(RECOVERY_SETTLE_DAYS), today).first()
            .filter { it.status == SessionStatus.GROWN || it.status == SessionStatus.WITHERED }
            .forEach { coins.settleSession(it.id) } // idempotent
    }

    /** Starts an allowed pass: pauses the focus clock and marks the tree as "took a break". */
    suspend fun beginPass(passRequestId: Long): Unit = lock.withLock {
        val now = clock.instant()
        val s = catchUp(checkNotNull(sessions.getOngoing()) { "No ongoing session" }, now)
        check(s.status == SessionStatus.ONGOING) { "The session already ended" }
        val pass = checkNotNull(passes.get(passRequestId)) { "No pass request $passRequestId" }
        check(pass.sessionId == s.id) { "Pass request $passRequestId belongs to another session" }
        check(pass.verdict == Verdict.ALLOW && pass.grantedMinutes > 0) { "Pass request $passRequestId was not allowed" }
        check(pass.passStartedAt == null) { "Pass $passRequestId was already started" }
        check(passes.activePass(s.id) == null) { "Another pass is already running" }
        check(s.passCount < MAX_PASSES) { "Pass limit ($MAX_PASSES) reached" }
        passes.update(pass.copy(passStartedAt = now))
        sessions.update(s.copy(pauseStartedAt = now, tookBreak = true, passCount = s.passCount + 1))
    }

    /** Ends the running pass now (no-op if none is running). See [closePass] for late returns. */
    suspend fun endPass(reason: PassEndReason): Unit = lock.withLock {
        val s = sessions.getOngoing() ?: return@withLock
        val pass = passes.activePass(s.id) ?: return@withLock
        closePass(s, pass, reason, clock.instant())
    }

    private suspend fun tickLocked(): Session? = sessions.getOngoing()?.let { catchUp(it, clock.instant()) }

    /**
     * Applies everything that is already true at [now] before any other action: closes a pass whose time ran out
     * (at its expiry), then completes a due timer / capped stopwatch. Returns the still-ongoing session or the
     * session it just finished, so callers never act on stale state.
     */
    private suspend fun catchUp(ongoing: Session, now: Instant): Session {
        val s = passes.activePass(ongoing.id)
            ?.takeIf { !now.isBefore(expiryOf(it)) }
            ?.let { closePass(ongoing, it, PassEndReason.EXPIRED, now) }
            ?: ongoing
        val due = when (s.mode) {
            SessionMode.TIMER -> SessionTiming.timerEnd(s, now)?.takeIf { !now.isBefore(it) }
            SessionMode.STOPWATCH -> capInstant(s, now)
        }
        return if (due != null) finish(s, SessionStatus.GROWN, null, due) else s
    }

    /**
     * Ends [pass] at [at] and resumes the focus clock. A pass can never outlast its grant: if [at] is past the
     * expiry, it is closed at the expiry instant with reason EXPIRED (whatever was asked), so returning late or
     * a tick after process death never counts extra minutes as paused or used.
     */
    private suspend fun closePass(s: Session, pass: PassRequest, reason: PassEndReason, at: Instant): Session {
        val start = checkNotNull(pass.passStartedAt)
        val expiry = expiryOf(pass)
        val (endAt, endReason) = if (at.isAfter(expiry)) expiry to PassEndReason.EXPIRED else at to reason
        passes.update(
            pass.copy(
                passEndedAt = endAt,
                passEndReason = endReason,
                usedSeconds = nonNegativeSeconds(start, endAt).coerceAtMost(pass.grantedMinutes * 60L),
                returnedBeforeExpiry = endReason == PassEndReason.RETURNED_TO_ROOTD,
            ),
        )
        val resumed = s.copy(
            pausedSeconds = s.pausedSeconds + nonNegativeSeconds(s.pauseStartedAt ?: start, endAt),
            pauseStartedAt = null,
        )
        sessions.update(resumed)
        return resumed
    }

    private fun expiryOf(pass: PassRequest): Instant =
        checkNotNull(pass.passStartedAt) + Duration.ofMinutes(pass.grantedMinutes.toLong())

    private fun nonNegativeSeconds(from: Instant, to: Instant): Long =
        Duration.between(from, to).seconds.coerceAtLeast(0)

    /** The instant a stopwatch reached the 120-minute cap, or null if it hasn't yet. */
    private fun capInstant(s: Session, now: Instant): Instant? {
        val cap = Duration.ofMinutes(SessionTiming.STOPWATCH_CAP_MINUTES.toLong())
        val focus = SessionTiming.elapsedFocus(s, now)
        return if (focus >= cap) now - (focus - cap) else null
    }

    /** The only way a session becomes grown/withered. Closes a running pass, then settles coins exactly once. */
    private suspend fun finish(session: Session, status: SessionStatus, reason: FailReason?, finishAt: Instant): Session {
        val at = maxOf(finishAt, session.startedAt) // a clock that moved backwards must not end before the start
        val s = passes.activePass(session.id)?.let { closePass(session, it, PassEndReason.SESSION_ENDED, at) } ?: session
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

    private fun withinCancelWindow(s: Session, now: Instant): Boolean {
        val sinceStart = Duration.between(s.startedAt, now)
        // A clock moved backwards must not reopen the window on a long-running session.
        return !sinceStart.isNegative && sinceStart < Duration.ofSeconds(SessionTiming.CANCEL_WINDOW_SECONDS)
    }
}
