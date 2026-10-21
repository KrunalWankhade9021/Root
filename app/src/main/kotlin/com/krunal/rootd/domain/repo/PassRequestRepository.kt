package com.krunal.rootd.domain.repo

import com.krunal.rootd.domain.model.PassRequest
import java.time.LocalDate

interface PassRequestRepository {
    suspend fun insert(request: PassRequest): Long
    suspend fun update(request: PassRequest)
    suspend fun get(id: Long): PassRequest?

    /** Ordered by askedAt. */
    suspend fun forSession(sessionId: Long): List<PassRequest>

    /** The pass that is running now: passStartedAt != null && passEndedAt == null. */
    suspend fun activePass(sessionId: Long): PassRequest?
    suspend fun countNonAppealForAppOn(appPackage: String, date: LocalDate): Int

    /** Requests whose askedLocalDate is in [startDate, endDateInclusive]. */
    suspend fun between(startDate: LocalDate, endDateInclusive: LocalDate): List<PassRequest>
}
