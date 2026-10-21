package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.krunal.rootd.data.db.entity.AllowListAppEntity
import com.krunal.rootd.data.db.entity.AllowListEntity
import com.krunal.rootd.data.db.entity.AlwaysAllowedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AllowListDao {
    @Query("SELECT * FROM allow_lists ORDER BY id")
    abstract fun observeAllowLists(): Flow<List<AllowListEntity>>

    @Query("SELECT * FROM allow_lists WHERE id = :id")
    abstract suspend fun get(id: Long): AllowListEntity?

    @Query("SELECT * FROM allow_lists WHERE is_default = 1 LIMIT 1")
    abstract suspend fun getDefault(): AllowListEntity?

    @Query("SELECT COUNT(*) FROM allow_lists")
    abstract suspend fun count(): Int

    @Query("SELECT * FROM allow_lists WHERE lower(name) = lower(:name) AND id != :excludeId LIMIT 1")
    abstract suspend fun findByName(name: String, excludeId: Long): AllowListEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(list: AllowListEntity): Long

    @Query("UPDATE allow_lists SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String)

    /** Raw delete; the "in use / last list" checks belong to the repository (T04). Apps cascade. */
    @Query("DELETE FROM allow_lists WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** No-op for an unknown id, so a stale id can never leave zero defaults (data-model §5.3). */
    @Query("UPDATE allow_lists SET is_default = (id = :id) WHERE EXISTS (SELECT 1 FROM allow_lists WHERE id = :id)")
    protected abstract suspend fun markOnlyDefault(id: Long)

    /** Makes [id] the single default list. */
    @Transaction
    open suspend fun setDefault(id: Long) = markOnlyDefault(id)

    @Query("SELECT app_package FROM allow_list_apps WHERE allow_list_id = :allowListId")
    abstract suspend fun packagesFor(allowListId: Long): List<String>

    @Query("DELETE FROM allow_list_apps WHERE allow_list_id = :allowListId")
    protected abstract suspend fun clearPackages(allowListId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertPackages(apps: List<AllowListAppEntity>)

    /** Replaces the list's packages atomically. */
    @Transaction
    open suspend fun setPackages(allowListId: Long, packages: Set<String>) {
        clearPackages(allowListId)
        insertPackages(packages.map { AllowListAppEntity(allowListId, it) })
    }

    @Query("SELECT * FROM always_allowed_apps ORDER BY app_package")
    abstract fun observeAlwaysAllowed(): Flow<List<AlwaysAllowedAppEntity>>

    @Query("SELECT app_package FROM always_allowed_apps")
    abstract suspend fun alwaysAllowedPackages(): List<String>

    @Query("SELECT * FROM always_allowed_apps WHERE app_package = :appPackage")
    abstract suspend fun getAlwaysAllowed(appPackage: String): AlwaysAllowedAppEntity?

    /** Ignored if the package is already present (data-model §5.5). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAlwaysAllowed(app: AlwaysAllowedAppEntity)

    @Query("DELETE FROM always_allowed_apps WHERE app_package = :appPackage")
    abstract suspend fun deleteAlwaysAllowed(appPackage: String)
}
