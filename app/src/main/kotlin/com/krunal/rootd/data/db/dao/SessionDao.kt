package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.krunal.rootd.data.db.SessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(sessions: List<SessionEntity>): List<Long>

    @Update
    suspend fun update(session: SessionEntity): Int

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ongoing'")
    suspend fun getOngoing(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ongoing'")
    fun observeOngoing(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE started_local_date BETWEEN :startDate AND :endDateInclusive ORDER BY started_at ASC")
    fun observeBetween(startDate: String, endDateInclusive: String): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE status IN ('grown', 'withered')")
    suspend fun countFinished(): Int

    @Query("SELECT MIN(started_local_date) FROM sessions")
    suspend fun earliestStartDate(): String?

    @Delete
    suspend fun delete(session: SessionEntity): Int

    @Transaction
    suspend fun deleteWithRelations(session: SessionEntity) {
        delete(session)
    }
}