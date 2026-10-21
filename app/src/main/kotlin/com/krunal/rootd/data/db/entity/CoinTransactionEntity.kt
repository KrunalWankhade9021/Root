package com.krunal.rootd.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.krunal.rootd.domain.model.CoinKind
import com.krunal.rootd.domain.model.CoinTransaction
import java.time.Instant

/** data-model §6. */
@Entity(
    tableName = "coin_transactions",
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = SpeciesEntity::class, parentColumns = ["id"], childColumns = ["species_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("session_id"), Index("at"), Index("species_id")],
)
data class CoinTransactionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "at") val at: Instant,
    @ColumnInfo(name = "amount") val amount: Int,
    @ColumnInfo(name = "kind") val kind: CoinKind,
    @ColumnInfo(name = "session_id") val sessionId: Long?,
    @ColumnInfo(name = "species_id") val speciesId: Long?,
    @ColumnInfo(name = "note") val note: String?,
)

fun CoinTransactionEntity.toModel() =
    CoinTransaction(id = id, at = at, amount = amount, kind = kind, sessionId = sessionId, speciesId = speciesId, note = note)
fun CoinTransaction.toEntity() =
    CoinTransactionEntity(id = id, at = at, amount = amount, kind = kind, sessionId = sessionId, speciesId = speciesId, note = note)
