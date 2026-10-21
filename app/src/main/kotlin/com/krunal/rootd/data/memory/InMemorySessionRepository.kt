package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.Session
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.repo.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class InMemorySessionRepository : SessionRepository {
    private val rows = MutableStateFlow<Map<Long, Session>>(emptyMap())
    private val lock = Mutex()
    private var nextId = 1L

    override suspend fun insert(session: Session): Long = lock.withLock {
        if (session.status == SessionStatus.ONGOING && rows.value.values.any { it.status == SessionStatus.ONGOING }) {
            throw IllegalStateException("An ongoing session already exists")
        }
        val id = nextId++
        rows.value = rows.value + (id to session.copy(id = id))
        id
    }

    override suspend fun update(session: Session) = lock.withLock {
        require(rows.value.containsKey(session.id)) { "No session with id ${session.id}" }
        if (session.status == SessionStatus.ONGOING &&
            rows.value.values.any { it.status == SessionStatus.ONGOING && it.id != session.id }
        ) {
            throw IllegalStateException("An ongoing session already exists")
        }
        rows.value = rows.value + (session.id to session)
    }

    override suspend fun get(id: Long): Session? = rows.value[id]

    override suspend fun getOngoing(): Session? = rows.value.values.firstOrNull { it.status == SessionStatus.ONGOING }

    override fun observeOngoing(): Flow<Session?> =
        rows.map { all -> all.values.firstOrNull { it.status == SessionStatus.ONGOING } }

    override fun observeBetween(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<Session>> =
        rows.map { all ->
            all.values
                .filter { it.startedLocalDate in startDate..endDateInclusive }
                .sortedBy { it.startedAt }
        }

    override suspend fun countFinished(): Int =
        rows.value.values.count { it.status == SessionStatus.GROWN || it.status == SessionStatus.WITHERED }

    override suspend fun earliestStartDate(): LocalDate? = rows.value.values.minOfOrNull { it.startedLocalDate }
}
