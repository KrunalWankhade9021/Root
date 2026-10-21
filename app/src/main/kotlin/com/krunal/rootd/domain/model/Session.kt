package com.krunal.rootd.domain.model

import java.time.Instant
import java.time.LocalDate

/** One focus session = one tree. Mirrors data-model §3 (ms timestamps → Instant, local dates → LocalDate). */
data class Session(
    val id: Long = 0,
    val mode: SessionMode,
    val plannedMinutes: Int?,
    val startedAt: Instant,
    val startedLocalDate: LocalDate,
    val endedAt: Instant? = null,
    val status: SessionStatus = SessionStatus.ONGOING,
    val failReason: FailReason? = null,
    val pausedSeconds: Long = 0,
    val pauseStartedAt: Instant? = null,
    val focusSeconds: Long = 0,
    val speciesId: Long,
    val tagId: Long?,
    val allowListId: Long?,
    val goalNote: String?,
    val tookBreak: Boolean = false,
    val passCount: Int = 0,
    val requestCount: Int = 0,
    val lastDeniedAt: Instant? = null,
)
