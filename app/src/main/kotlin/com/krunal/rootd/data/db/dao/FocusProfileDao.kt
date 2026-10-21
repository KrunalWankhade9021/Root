package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.krunal.rootd.data.db.entity.FocusProfileEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface FocusProfileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: FocusProfileEntity): Long

    @Query("SELECT * FROM focus_profiles ORDER BY generated_at DESC LIMIT 1")
    suspend fun latest(): FocusProfileEntity?

    @Query("SELECT * FROM focus_profiles ORDER BY generated_at DESC LIMIT 1")
    fun observeLatest(): Flow<FocusProfileEntity?>

    @Query("UPDATE focus_profiles SET seen_at = :at WHERE id = :id")
    suspend fun markSeen(id: Long, at: Instant)
}
