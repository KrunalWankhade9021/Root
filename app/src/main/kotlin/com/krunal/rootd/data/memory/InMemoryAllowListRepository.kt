package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.AllowList
import com.krunal.rootd.domain.model.AlwaysAllowedApp
import com.krunal.rootd.domain.repo.AllowListRepository
import com.krunal.rootd.domain.repo.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock

/** Starts with one default list named "Default" (data-model §5.3); always-allowed apps start empty (seeded by T12). */
class InMemoryAllowListRepository(
    private val sessions: SessionRepository,
    private val clock: Clock,
) : AllowListRepository {
    private val lock = Mutex()
    private val lists = MutableStateFlow(
        mapOf(1L to AllowList(id = 1, name = "Default", isDefault = true, createdAt = clock.instant())),
    )
    private val packages = mutableMapOf<Long, Set<String>>()
    private val alwaysAllowed = MutableStateFlow<Map<String, AlwaysAllowedApp>>(emptyMap())
    private var nextId = 2L

    override fun observeAllowLists(): Flow<List<AllowList>> = lists.map { all -> all.values.sortedBy { it.id } }

    override suspend fun create(name: String): Long = lock.withLock {
        val id = nextId++
        lists.value = lists.value + (id to AllowList(id = id, name = name, isDefault = false, createdAt = clock.instant()))
        id
    }

    override suspend fun rename(id: Long, name: String) = lock.withLock {
        val list = lists.value[id] ?: return@withLock
        lists.value = lists.value + (id to list.copy(name = name))
    }

    override suspend fun delete(id: Long): Boolean = lock.withLock {
        val list = lists.value[id] ?: return@withLock false
        if (lists.value.size <= 1) return@withLock false
        if (sessions.getOngoing()?.allowListId == id) return@withLock false
        var remaining = lists.value - id
        if (list.isDefault) {
            val newDefault = remaining.values.minBy { it.id }
            remaining = remaining + (newDefault.id to newDefault.copy(isDefault = true))
        }
        lists.value = remaining
        packages.remove(id)
        true
    }

    override suspend fun setDefault(id: Long) = lock.withLock {
        if (!lists.value.containsKey(id)) return@withLock
        lists.value = lists.value.mapValues { (key, list) -> list.copy(isDefault = key == id) }
    }

    override suspend fun getDefault(): AllowList = lists.value.values.first { it.isDefault }

    override suspend fun packagesFor(allowListId: Long): Set<String> = lock.withLock { packages[allowListId].orEmpty() }

    override suspend fun setPackages(allowListId: Long, packages: Set<String>) = lock.withLock {
        this.packages[allowListId] = packages.toSet()
    }

    override fun observeAlwaysAllowed(): Flow<List<AlwaysAllowedApp>> =
        alwaysAllowed.map { all -> all.values.sortedBy { it.appPackage } }

    override suspend fun alwaysAllowedPackages(): Set<String> = alwaysAllowed.value.keys

    override suspend fun addAlwaysAllowed(app: AlwaysAllowedApp) = lock.withLock {
        if (alwaysAllowed.value.containsKey(app.appPackage)) return@withLock
        alwaysAllowed.value = alwaysAllowed.value + (app.appPackage to app)
    }

    override suspend fun removeAlwaysAllowed(appPackage: String): Boolean = lock.withLock {
        val app = alwaysAllowed.value[appPackage] ?: return@withLock false
        if (app.isSystemDefault) return@withLock false
        alwaysAllowed.value = alwaysAllowed.value - appPackage
        true
    }
}
