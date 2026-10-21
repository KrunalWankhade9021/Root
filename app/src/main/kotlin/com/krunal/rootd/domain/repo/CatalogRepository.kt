package com.krunal.rootd.domain.repo

import com.krunal.rootd.domain.model.Species
import com.krunal.rootd.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface CatalogRepository {
    fun observeTags(includeArchived: Boolean = false): Flow<List<Tag>>
    suspend fun getTag(id: Long): Tag?

    /** Throws [IllegalArgumentException] if another non-archived tag has the same name (case-insensitive). */
    suspend fun upsertTag(tag: Tag): Long
    suspend fun setTagArchived(id: Long, archived: Boolean)

    /** Ordered by sortOrder. */
    fun observeSpecies(): Flow<List<Species>>
    suspend fun getSpecies(id: Long): Species?
    suspend fun markSpeciesUnlocked(id: Long, at: Instant)
}
