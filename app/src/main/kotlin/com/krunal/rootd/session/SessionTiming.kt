package com.krunal.rootd.session

import com.krunal.rootd.domain.model.Session
import com.krunal.rootd.domain.model.SessionMode
import java.time.Duration
import java.time.Instant

enum class GrowthStage { SEED, SPROUT, SAPLING, TREE }

/** Session time maths from data-model §3.2. Pure functions of a session row and "now". */
object SessionTiming {
    const val CANCEL_WINDOW_SECONDS = 10L
    const val STOPWATCH_CAP_MINUTES = 120
    const val STOPWATCH_MIN_MINUTES = 10

    /**
     * Focused time so far: wall time since start minus finished and current pauses, clamped to
     * [0, wall time] so a clock moving backwards or a corrupted row can never produce negative or inflated focus.
     */
    fun elapsedFocus(s: Session, now: Instant): Duration {
        val wall = Duration.between(s.startedAt, now).coerceAtLeast(Duration.ZERO)
        val currentPause = s.pauseStartedAt
            ?.let { Duration.between(it, now).coerceAtLeast(Duration.ZERO) }
            ?: Duration.ZERO
        val focus = wall - Duration.ofSeconds(s.pausedSeconds) - currentPause
        return focus.coerceIn(Duration.ZERO, wall)
    }

    /** When a timer session completes: start + planned + all pause time (including a pause running now). */
    fun timerEnd(s: Session, now: Instant): Instant? {
        if (s.mode != SessionMode.TIMER) return null
        val planned = s.plannedMinutes ?: return null
        val currentPause = s.pauseStartedAt
            ?.let { Duration.between(it, now).coerceAtLeast(Duration.ZERO) }
            ?: Duration.ZERO
        return s.startedAt + Duration.ofMinutes(planned.toLong()) + Duration.ofSeconds(s.pausedSeconds) + currentPause
    }

    /** UI growth stage by share of the target: planned minutes (timer) or the 120-minute cap (stopwatch). */
    fun growthStage(s: Session, now: Instant): GrowthStage {
        val targetSeconds = 60L * (if (s.mode == SessionMode.TIMER) s.plannedMinutes ?: STOPWATCH_CAP_MINUTES else STOPWATCH_CAP_MINUTES)
        val percent = elapsedFocus(s, now).seconds * 100 / targetSeconds
        return when {
            percent < 25 -> GrowthStage.SEED
            percent < 50 -> GrowthStage.SPROUT
            percent < 75 -> GrowthStage.SAPLING
            else -> GrowthStage.TREE
        }
    }
}
