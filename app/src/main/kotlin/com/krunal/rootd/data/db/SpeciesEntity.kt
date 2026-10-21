package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "species",
    indices = [
        Index(value = ["key"], unique = true),
        Index(value = ["sort_order"])
    ]
)
data class SpeciesEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price_coins") val priceCoins: Int,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
) {
    fun toModel(): com.krunal.rootd.domain.model.Species {
        return com.krunal.rootd.domain.model.Species(
            id = id,
            key = key,
            name = name,
            priceCoins = priceCoins,
            unlockedAt = unlockedAt?.let { Instant.ofEpochMilli(it) },
            sortOrder = sortOrder
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.Species): SpeciesEntity {
            return SpeciesEntity(
                id = model.id,
                key = model.key,
                name = model.name,
                priceCoins = model.priceCoins,
                unlockedAt = model.unlockedAt?.toEpochMilli(),
                sortOrder = model.sortOrder
            )
        }
    }
}