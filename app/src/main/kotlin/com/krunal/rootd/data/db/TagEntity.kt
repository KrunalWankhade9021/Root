package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_argb") val colorArgb: Int,
    @ColumnInfo(name = "archived") val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    fun toModel(): com.krunal.rootd.domain.model.Tag {
        return com.krunal.rootd.domain.model.Tag(
            id = id,
            name = name,
            colorArgb = colorArgb,
            archived = archived,
            createdAt = Instant.ofEpochMilli(createdAt)
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.Tag): TagEntity {
            return TagEntity(
                id = model.id,
                name = model.name,
                colorArgb = model.colorArgb,
                archived = model.archived,
                createdAt = model.createdAt.toEpochMilli()
            )
        }
    }
}