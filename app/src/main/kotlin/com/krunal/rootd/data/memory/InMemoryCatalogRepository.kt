package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.Species
import com.krunal.rootd.domain.model.Tag
import com.krunal.rootd.domain.repo.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant

/** Tags and species, seeded like the Room database (data-model §5.1, §5.2). */
class InMemoryCatalogRepository(clock: Clock) : CatalogRepository {
    private val lock = Mutex()
    private val tags: MutableStateFlow<Map<Long, Tag>>
    private val species: MutableStateFlow<Map<Long, Species>>
    private var nextTagId: Long

    init {
        val now = clock.instant()
        val seedTags = listOf(
            "study" to 0xFF4A7FD1.toInt(),
            "work" to 0xFFE09A3E.toInt(),
            "read" to 0xFF9B6FD1.toInt(),
            "exercise" to 0xFF2E7D4F.toInt(),
        ).mapIndexed { i, (name, color) -> Tag(id = i + 1L, name = name, colorArgb = color, createdAt = now) }
        tags = MutableStateFlow(seedTags.associateBy { it.id })
        nextTagId = seedTags.size + 1L

        val seedSpecies = listOf(
            Triple("oak", "Oak", 0), Triple("pine", "Pine", 0), Triple("birch", "Birch", 300),
            Triple("maple", "Maple", 500), Triple("cherry", "Cherry", 800), Triple("bamboo", "Bamboo", 1000),
            Triple("baobab", "Baobab", 1500),
        ).mapIndexed { i, (key, name, price) ->
            Species(
                id = i + 1L, key = key, name = name, priceCoins = price,
                unlockedAt = if (price == 0) now else null, sortOrder = i,
            )
        }
        species = MutableStateFlow(seedSpecies.associateBy { it.id })
    }

    override fun observeTags(includeArchived: Boolean): Flow<List<Tag>> =
        tags.map { all -> all.values.filter { includeArchived || !it.archived }.sortedBy { it.id } }

    override suspend fun getTag(id: Long): Tag? = tags.value[id]

    override suspend fun upsertTag(tag: Tag): Long = lock.withLock {
        val clash = tags.value.values.any {
            !it.archived && it.id != tag.id && it.name.equals(tag.name, ignoreCase = true)
        }
        if (clash) throw IllegalArgumentException("A tag named '${tag.name}' already exists")
        val id = if (tag.id == 0L) nextTagId++ else tag.id
        tags.value = tags.value + (id to tag.copy(id = id))
        id
    }

    override suspend fun setTagArchived(id: Long, archived: Boolean) = lock.withLock {
        val tag = tags.value[id] ?: return@withLock
        tags.value = tags.value + (id to tag.copy(archived = archived))
    }

    override fun observeSpecies(): Flow<List<Species>> = species.map { all -> all.values.sortedBy { it.sortOrder } }

    override suspend fun getSpecies(id: Long): Species? = species.value[id]

    override suspend fun markSpeciesUnlocked(id: Long, at: Instant) = lock.withLock {
        val s = species.value[id] ?: return@withLock
        species.value = species.value + (id to s.copy(unlockedAt = at))
    }
}
