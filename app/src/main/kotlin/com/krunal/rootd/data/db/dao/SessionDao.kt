package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.krunal.rootd.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SessionDao {
    /** Throws SQLiteConstraintException if another session is ongoing (partial unique index, data-model §8). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ongoing' LIMIT 1")
    suspend fun getOngoing(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ongoing' LIMIT 1")
    fun observeOngoing(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE started_local_date BETWEEN :start AND :endInclusive ORDER BY started_at")
    fun observeBetween(start: LocalDate, endInclusive: LocalDate): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE status IN ('grown', 'withered')")
    suspend fun countFinished(): Int

    @Query("SELECT MIN(started_local_date) FROM sessions")
    suspend fun earliestStartDate(): LocalDate?
}
