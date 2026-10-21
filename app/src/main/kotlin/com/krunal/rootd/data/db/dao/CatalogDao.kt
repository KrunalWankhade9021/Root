package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.krunal.rootd.data.db.SpeciesEntity
import com.krunal.rootd.data.db.TagEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CatalogDao {

    @Query("SELECT * FROM tags WHERE archived = 0 ORDER BY id ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY id ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTag(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity): Int

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long): Int

    @Query("UPDATE tags SET archived = :archived WHERE id = :id")
    suspend fun setTagArchived(id: Long, archived: Boolean): Int

    @Query("SELECT * FROM species ORDER BY sort_order ASC")
    fun observeSpecies(): Flow<List<SpeciesEntity>>

    @Query("SELECT * FROM species WHERE id = :id")
    suspend fun getSpecies(id: Long): SpeciesEntity?

    @Query("UPDATE species SET unlocked_at = :unlockedAt WHERE id = :id")
    suspend fun markSpeciesUnlocked(id: Long, unlockedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSpecies(species: SpeciesEntity): Long

    @Query("SELECT COUNT(*) FROM tags WHERE name = :name COLLATE NOCASE AND archived = 0 AND id != :excludeId")
    suspend fun countActiveTagByName(name: String, excludeId: Long): Int
}