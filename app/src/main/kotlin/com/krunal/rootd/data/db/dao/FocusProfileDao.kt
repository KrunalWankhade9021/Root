package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.krunal.rootd.data.db.FocusProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusProfileDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: FocusProfileEntity): Long

    @Update
    suspend fun update(profile: FocusProfileEntity): Int

    @Query("SELECT * FROM focus_profiles ORDER BY generated_at DESC LIMIT 1")
    suspend fun latest(): FocusProfileEntity?

    @Query("SELECT * FROM focus_profiles ORDER BY generated_at DESC LIMIT 1")
    fun observeLatest(): Flow<FocusProfileEntity?>

    @Query("SELECT * FROM focus_profiles WHERE id = :id")
    suspend fun get(id: Long): FocusProfileEntity?

    @Query("UPDATE focus_profiles SET seen_at = :seenAt WHERE id = :id")
    suspend fun markSeen(id: Long, seenAt: Long): Int
}