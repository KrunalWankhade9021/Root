package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "focus_profiles",
    indices = [Index(value = ["generated_at"])]
)
data class FocusProfileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "period_start_local_date") val periodStart: String,
    @ColumnInfo(name = "period_end_local_date") val periodEnd: String,
    @ColumnInfo(name = "profile_text") val profileText: String,
    @ColumnInfo(name = "insights_json") val insightsJson: String,
    @ColumnInfo(name = "stats_snapshot_json") val statsSnapshotJson: String,
    @ColumnInfo(name = "llm_model") val llmModel: String,
    @ColumnInfo(name = "seen_at") val seenAt: Long?,
) {
    fun toModel(): com.krunal.rootd.domain.model.FocusProfile {
        return com.krunal.rootd.domain.model.FocusProfile(
            id = id,
            generatedAt = Instant.ofEpochMilli(generatedAt),
            periodStart = LocalDate.parse(periodStart),
            periodEnd = LocalDate.parse(periodEnd),
            profileText = profileText,
            insightsJson = insightsJson,
            statsSnapshotJson = statsSnapshotJson,
            llmModel = llmModel,
            seenAt = seenAt?.let { Instant.ofEpochMilli(it) }
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.FocusProfile): FocusProfileEntity {
            return FocusProfileEntity(
                id = model.id,
                generatedAt = model.generatedAt.toEpochMilli(),
                periodStart = model.periodStart.toString(),
                periodEnd = model.periodEnd.toString(),
                profileText = model.profileText,
                insightsJson = model.insightsJson,
                statsSnapshotJson = model.statsSnapshotJson,
                llmModel = model.llmModel,
                seenAt = model.seenAt?.toEpochMilli()
            )
        }
    }
}