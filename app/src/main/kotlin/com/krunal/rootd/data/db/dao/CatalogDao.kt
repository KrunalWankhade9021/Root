package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.krunal.rootd.data.db.entity.SpeciesEntity
import com.krunal.rootd.data.db.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CatalogDao {
    @Query("SELECT * FROM tags WHERE :includeArchived OR archived = 0 ORDER BY id")
    fun observeTags(includeArchived: Boolean): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTag(id: Long): TagEntity?

    /** For the "unique among non-archived, case-insensitive" rule (data-model §5.1). */
    @Query("SELECT * FROM tags WHERE archived = 0 AND lower(name) = lower(:name) AND id != :excludeId LIMIT 1")
    suspend fun findActiveTagByName(name: String, excludeId: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("UPDATE tags SET archived = :archived WHERE id = :id")
    suspend fun setTagArchived(id: Long, archived: Boolean)

    /** Normal UI archives tags; hard delete exists for completeness (sessions keep history via ON DELETE SET NULL). */
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query("SELECT * FROM species ORDER BY sort_order")
    fun observeSpecies(): Flow<List<SpeciesEntity>>

    @Query("SELECT * FROM species WHERE id = :id")
    suspend fun getSpecies(id: Long): SpeciesEntity?

    @Query("UPDATE species SET unlocked_at = :at WHERE id = :id")
    suspend fun markSpeciesUnlocked(id: Long, at: Instant)
}
