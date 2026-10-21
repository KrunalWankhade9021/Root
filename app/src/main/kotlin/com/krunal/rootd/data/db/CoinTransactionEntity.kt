package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.krunal.rootd.domain.model.CoinKind
import java.time.Instant

@Entity(
    tableName = "coin_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SpeciesEntity::class,
            parentColumns = ["id"],
            childColumns = ["species_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["at"]),
        Index(value = ["species_id"])
    ]
)
data class CoinTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "at") val at: Long,
    @ColumnInfo(name = "amount") val amount: Int,
    @ColumnInfo(name = "kind") val kind: CoinKind,
    @ColumnInfo(name = "session_id") val sessionId: Long?,
    @ColumnInfo(name = "species_id") val speciesId: Long?,
    @ColumnInfo(name = "note") val note: String?,
) {
    fun toModel(): com.krunal.rootd.domain.model.CoinTransaction {
        return com.krunal.rootd.domain.model.CoinTransaction(
            id = id,
            at = Instant.ofEpochMilli(at),
            amount = amount,
            kind = kind,
            sessionId = sessionId,
            speciesId = speciesId,
            note = note
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.CoinTransaction): CoinTransactionEntity {
            return CoinTransactionEntity(
                id = model.id,
                at = model.at.toEpochMilli(),
                amount = model.amount,
                kind = model.kind,
                sessionId = model.sessionId,
                speciesId = model.speciesId,
                note = model.note
            )
        }
    }
}