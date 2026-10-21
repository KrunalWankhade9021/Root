package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.krunal.rootd.data.db.entity.PassRequestEntity
import java.time.LocalDate

@Dao
interface PassRequestDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(request: PassRequestEntity): Long

    @Update
    suspend fun update(request: PassRequestEntity)

    @Query("SELECT * FROM pass_requests WHERE id = :id")
    suspend fun get(id: Long): PassRequestEntity?

    @Query("SELECT * FROM pass_requests WHERE session_id = :sessionId ORDER BY asked_at")
    suspend fun forSession(sessionId: Long): List<PassRequestEntity>

    /** The pass running now: started and not ended (data-model §4.1-7: at most one per session). */
    @Query(
        "SELECT * FROM pass_requests WHERE session_id = :sessionId " +
            "AND pass_started_at IS NOT NULL AND pass_ended_at IS NULL LIMIT 1",
    )
    suspend fun activePass(sessionId: Long): PassRequestEntity?

    @Query(
        "SELECT COUNT(*) FROM pass_requests WHERE app_package = :appPackage " +
            "AND asked_local_date = :date AND was_appeal = 0",
    )
    suspend fun countNonAppealForAppOn(appPackage: String, date: LocalDate): Int

    @Query("SELECT * FROM pass_requests WHERE asked_local_date BETWEEN :start AND :endInclusive ORDER BY asked_at")
    suspend fun between(start: LocalDate, endInclusive: LocalDate): List<PassRequestEntity>
}
