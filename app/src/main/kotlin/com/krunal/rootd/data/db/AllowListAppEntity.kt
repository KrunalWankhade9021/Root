package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "allow_list_apps",
    primaryKeys = ["allow_list_id", "app_package"],
    foreignKeys = [
        ForeignKey(
            entity = AllowListEntity::class,
            parentColumns = ["id"],
            childColumns = ["allow_list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AllowListAppEntity(
    @ColumnInfo(name = "allow_list_id") val allowListId: Long,
    @ColumnInfo(name = "app_package") val appPackage: String,
)