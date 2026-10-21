package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.PassRequest
import com.krunal.rootd.domain.repo.PassRequestRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class InMemoryPassRequestRepository : PassRequestRepository {
    private var rows: Map<Long, PassRequest> = emptyMap()
    private val lock = Mutex()
    private var nextId = 1L

    override suspend fun insert(request: PassRequest): Long = lock.withLock {
        val id = nextId++
        rows = rows + (id to request.copy(id = id))
        id
    }

    override suspend fun update(request: PassRequest) = lock.withLock {
        require(rows.containsKey(request.id)) { "No pass request with id ${request.id}" }
        rows = rows + (request.id to request)
    }

    override suspend fun get(id: Long): PassRequest? = rows[id]

    override suspend fun forSession(sessionId: Long): List<PassRequest> =
        rows.values.filter { it.sessionId == sessionId }.sortedBy { it.askedAt }

    override suspend fun activePass(sessionId: Long): PassRequest? =
        rows.values.firstOrNull { it.sessionId == sessionId && it.passStartedAt != null && it.passEndedAt == null }

    override suspend fun countNonAppealForAppOn(appPackage: String, date: LocalDate): Int =
        rows.values.count { it.appPackage == appPackage && it.askedLocalDate == date && !it.wasAppeal }

    override suspend fun between(startDate: LocalDate, endDateInclusive: LocalDate): List<PassRequest> =
        rows.values.filter { it.askedLocalDate in startDate..endDateInclusive }.sortedBy { it.askedAt }
}
