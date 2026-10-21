package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "allow_lists",
    indices = [Index(value = ["name"], unique = true)]
)
data class AllowListEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    fun toModel(): com.krunal.rootd.domain.model.AllowList {
        return com.krunal.rootd.domain.model.AllowList(
            id = id,
            name = name,
            isDefault = isDefault,
            createdAt = Instant.ofEpochMilli(createdAt)
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.AllowList): AllowListEntity {
            return AllowListEntity(
                id = model.id,
                name = model.name,
                isDefault = model.isDefault,
                createdAt = model.createdAt.toEpochMilli()
            )
        }
    }
}