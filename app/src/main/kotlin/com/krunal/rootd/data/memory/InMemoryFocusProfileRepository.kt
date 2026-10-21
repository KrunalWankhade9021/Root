package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.FocusProfile
import com.krunal.rootd.domain.repo.FocusProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class InMemoryFocusProfileRepository : FocusProfileRepository {
    private val lock = Mutex()
    private val rows = MutableStateFlow<List<FocusProfile>>(emptyList())
    private var nextId = 1L

    override suspend fun latest(): FocusProfile? = rows.value.maxByOrNull { it.generatedAt }

    override fun observeLatest(): Flow<FocusProfile?> = rows.map { all -> all.maxByOrNull { it.generatedAt } }

    override suspend fun insert(profile: FocusProfile): Long = lock.withLock {
        val id = nextId++
        rows.value = rows.value + profile.copy(id = id)
        id
    }

    override suspend fun markSeen(id: Long, at: Instant) = lock.withLock {
        rows.value = rows.value.map { if (it.id == id) it.copy(seenAt = at) else it }
    }
}
