package com.krunal.rootd.domain.model

import java.time.Instant
import java.time.LocalDate

/** Weekly LLM profile (data-model §7). JSON columns stay raw here; parsing lives in T14/T15. */
data class FocusProfile(
    val id: Long = 0,
    val generatedAt: Instant,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val profileText: String,
    val insightsJson: String,
    val statsSnapshotJson: String,
    val llmModel: String,
    val seenAt: Instant?,
)
