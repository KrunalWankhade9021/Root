package com.krunal.rootd.domain.repo

import com.krunal.rootd.domain.model.FocusProfile
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface FocusProfileRepository {
    /** The profile with the latest generatedAt, or null if none exists. */
    suspend fun latest(): FocusProfile?
    fun observeLatest(): Flow<FocusProfile?>
    suspend fun insert(profile: FocusProfile): Long
    suspend fun markSeen(id: Long, at: Instant)
}
