package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.krunal.rootd.data.db.AllowListAppEntity
import com.krunal.rootd.data.db.AllowListEntity
import com.krunal.rootd.data.db.AlwaysAllowedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowListDao {

    @Query("SELECT * FROM allow_lists ORDER BY id ASC")
    fun observeAllowLists(): Flow<List<AllowListEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllowList(list: AllowListEntity): Long

    @Update
    suspend fun updateAllowList(list: AllowListEntity): Int

    @Query("DELETE FROM allow_lists WHERE id = :id")
    suspend fun deleteAllowList(id: Long): Int

    @Query("SELECT * FROM allow_lists WHERE is_default = 1")
    suspend fun getDefault(): AllowListEntity?

    @Query("UPDATE allow_lists SET is_default = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefault(id: Long): Int

    @Query("SELECT COUNT(*) FROM allow_lists")
    suspend fun countAllowLists(): Int

    @Query("SELECT * FROM allow_list_apps WHERE allow_list_id = :allowListId")
    suspend fun getPackagesFor(allowListId: Long): List<AllowListAppEntity>

    @Transaction
    suspend fun setPackages(allowListId: Long, packages: Set<String>) {
        // Delete existing
        allowListAppDao().deleteByAllowListId(allowListId)
        // Insert new
        if (packages.isNotEmpty()) {
            allowListAppDao().insertAll(packages.map { AllowListAppEntity(allowListId, it) })
        }
    }

    @Query("SELECT * FROM always_allowed_apps ORDER BY app_package ASC")
    fun observeAlwaysAllowed(): Flow<List<AlwaysAllowedAppEntity>>

    @Query("SELECT app_package FROM always_allowed_apps")
    suspend fun alwaysAllowedPackages(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAlwaysAllowed(app: AlwaysAllowedAppEntity): Long

    @Query("DELETE FROM always_allowed_apps WHERE app_package = :appPackage AND is_system_default = 0")
    suspend fun removeAlwaysAllowed(appPackage: String): Int

    @Query("SELECT * FROM always_allowed_apps WHERE app_package = :appPackage")
    suspend fun getAlwaysAllowed(appPackage: String): AlwaysAllowedAppEntity?

    // Sub-DAOs for transactions
    abstract fun allowListAppDao(): AllowListAppDao
}