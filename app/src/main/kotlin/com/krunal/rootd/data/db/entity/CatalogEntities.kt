package com.krunal.rootd.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.krunal.rootd.domain.model.AllowList
import com.krunal.rootd.domain.model.AlwaysAllowedApp
import com.krunal.rootd.domain.model.AlwaysAllowedReason
import com.krunal.rootd.domain.model.Species
import com.krunal.rootd.domain.model.Tag
import java.time.Instant

/**
 * data-model §5.1. Name uniqueness is "among non-archived tags, case-insensitive" — a rule SQLite indexes can't
 * express without a partial index on lower(name), so the repository (T04) enforces it.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_argb") val colorArgb: Int,
    @ColumnInfo(name = "archived", defaultValue = "0") val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

fun TagEntity.toModel() = Tag(id = id, name = name, colorArgb = colorArgb, archived = archived, createdAt = createdAt)
fun Tag.toEntity() = TagEntity(id = id, name = name, colorArgb = colorArgb, archived = archived, createdAt = createdAt)

/** data-model §5.2. Fixed ids from the seed (not autoincrement). */
@Entity(tableName = "species", indices = [Index(value = ["key"], unique = true)])
data class SpeciesEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price_coins") val priceCoins: Int,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Instant?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

fun SpeciesEntity.toModel() =
    Species(id = id, key = key, name = name, priceCoins = priceCoins, unlockedAt = unlockedAt, sortOrder = sortOrder)
fun Species.toEntity() =
    SpeciesEntity(id = id, key = key, name = name, priceCoins = priceCoins, unlockedAt = unlockedAt, sortOrder = sortOrder)

/** data-model §5.3. Case-insensitive name uniqueness and "exactly one default" are enforced by the repository (T04). */
@Entity(tableName = "allow_lists")
data class AllowListEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_default", defaultValue = "0") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

fun AllowListEntity.toModel() = AllowList(id = id, name = name, isDefault = isDefault, createdAt = createdAt)
fun AllowList.toEntity() = AllowListEntity(id = id, name = name, isDefault = isDefault, createdAt = createdAt)

/** data-model §5.4. */
@Entity(
    tableName = "allow_list_apps",
    primaryKeys = ["allow_list_id", "app_package"],
    foreignKeys = [
        ForeignKey(entity = AllowListEntity::class, parentColumns = ["id"], childColumns = ["allow_list_id"], onDelete = ForeignKey.CASCADE),
    ],
)
data class AllowListAppEntity(
    @ColumnInfo(name = "allow_list_id") val allowListId: Long,
    @ColumnInfo(name = "app_package") val appPackage: String,
)

/** data-model §5.5. Seeded on device by T12, never in the database callback. */
@Entity(tableName = "always_allowed_apps")
data class AlwaysAllowedAppEntity(
    @PrimaryKey @ColumnInfo(name = "app_package") val appPackage: String,
    @ColumnInfo(name = "reason") val reason: AlwaysAllowedReason,
    @ColumnInfo(name = "is_system_default", defaultValue = "0") val isSystemDefault: Boolean = false,
)

fun AlwaysAllowedAppEntity.toModel() = AlwaysAllowedApp(appPackage = appPackage, reason = reason, isSystemDefault = isSystemDefault)
fun AlwaysAllowedApp.toEntity() = AlwaysAllowedAppEntity(appPackage = appPackage, reason = reason, isSystemDefault = isSystemDefault)
