package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.krunal.rootd.data.db.AllowListAppEntity

@Dao
interface AllowListAppDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apps: List<AllowListAppEntity>): List<Long>

    @Query("DELETE FROM allow_list_apps WHERE allow_list_id = :allowListId")
    suspend fun deleteByAllowListId(allowListId: Long): Int

    @Query("SELECT * FROM allow_list_apps WHERE allow_list_id = :allowListId")
    suspend fun getByAllowListId(allowListId: Long): List<AllowListAppEntity>
}