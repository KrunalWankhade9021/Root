package com.krunal.rootd.domain.repo

import com.krunal.rootd.domain.model.Session
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface SessionRepository {
    /** Throws [IllegalStateException] if an ongoing session already exists. */
    suspend fun insert(session: Session): Long
    suspend fun update(session: Session)
    suspend fun get(id: Long): Session?
    suspend fun getOngoing(): Session?
    fun observeOngoing(): Flow<Session?>

    /** Sessions whose startedLocalDate is in [startDate, endDateInclusive], ordered by startedAt. */
    fun observeBetween(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<Session>>

    /** Count of sessions with status grown or withered. */
    suspend fun countFinished(): Int
    suspend fun earliestStartDate(): LocalDate?
}
