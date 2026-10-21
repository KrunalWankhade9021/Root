package com.krunal.rootd.domain.model

import java.time.Instant
import java.time.LocalDate

/** Every press of "I need it" and every appeal. Mirrors data-model §4. */
data class PassRequest(
    val id: Long = 0,
    val sessionId: Long,
    val appPackage: String,
    val appLabel: String,
    val askedAt: Instant,
    val askedLocalDate: LocalDate,
    val reasonText: String,
    val reasonCategory: ReasonCategory = ReasonCategory.OTHER,
    val wasAppeal: Boolean = false,
    val originalRequestId: Long? = null,
    val verdict: Verdict,
    val verdictSource: VerdictSource,
    val llmMessage: String?,
    val llmModel: String?,
    val llmLatencyMs: Long?,
    val grantedMinutes: Int = 0,
    val passStartedAt: Instant? = null,
    val passEndedAt: Instant? = null,
    val passEndReason: PassEndReason? = null,
    val usedSeconds: Long = 0,
    val returnedBeforeExpiry: Boolean? = null,
)
