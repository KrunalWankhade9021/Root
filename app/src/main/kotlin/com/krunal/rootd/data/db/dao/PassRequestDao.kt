package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.krunal.rootd.data.db.entity.PassRequestEntity

@Dao
interface PassRequestDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(request: PassRequestEntity): Long

    @Query("SELECT * FROM pass_requests WHERE id = :id")
    suspend fun get(id: Long): PassRequestEntity?
}
