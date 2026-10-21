package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.krunal.rootd.data.db.entity.FocusProfileEntity

@Dao
interface FocusProfileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: FocusProfileEntity): Long
}
