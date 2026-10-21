package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.krunal.rootd.data.db.PassRequestEntity
import java.time.LocalDate

@Dao
interface PassRequestDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(request: PassRequestEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(requests: List<PassRequestEntity>): List<Long>

    @Update
    suspend fun update(request: PassRequestEntity): Int

    @Query("SELECT * FROM pass_requests WHERE id = :id")
    suspend fun get(id: Long): PassRequestEntity?

    @Query("SELECT * FROM pass_requests WHERE session_id = :sessionId ORDER BY asked_at ASC")
    suspend fun forSession(sessionId: Long): List<PassRequestEntity>

    @Query("SELECT * FROM pass_requests WHERE session_id = :sessionId AND pass_started_at IS NOT NULL AND pass_ended_at IS NULL")
    suspend fun activePass(sessionId: Long): PassRequestEntity?

    @Query("SELECT COUNT(*) FROM pass_requests WHERE app_package = :appPackage AND asked_local_date = :date AND was_appeal = 0")
    suspend fun countNonAppealForAppOn(appPackage: String, date: String): Int

    @Query("SELECT * FROM pass_requests WHERE asked_local_date BETWEEN :startDate AND :endDateInclusive ORDER BY asked_at ASC")
    suspend fun between(startDate: String, endDateInclusive: String): List<PassRequestEntity>

    @Delete
    suspend fun delete(request: PassRequestEntity): Int

    @Query("DELETE FROM pass_requests WHERE session_id = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long): Int

    @Transaction
    suspend fun deleteWithRelations(request: PassRequestEntity) {
        delete(request)
    }
}