package com.krunal.rootd.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.krunal.rootd.domain.model.FocusProfile
import java.time.Instant
import java.time.LocalDate

/** data-model §7. */
@Entity(tableName = "focus_profiles", indices = [Index("generated_at")])
data class FocusProfileEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "generated_at") val generatedAt: Instant,
    @ColumnInfo(name = "period_start_local_date") val periodStart: LocalDate,
    @ColumnInfo(name = "period_end_local_date") val periodEnd: LocalDate,
    @ColumnInfo(name = "profile_text") val profileText: String,
    @ColumnInfo(name = "insights_json") val insightsJson: String,
    @ColumnInfo(name = "stats_snapshot_json") val statsSnapshotJson: String,
    @ColumnInfo(name = "llm_model") val llmModel: String,
    @ColumnInfo(name = "seen_at") val seenAt: Instant?,
)

fun FocusProfileEntity.toModel() = FocusProfile(
    id = id, generatedAt = generatedAt, periodStart = periodStart, periodEnd = periodEnd, profileText = profileText,
    insightsJson = insightsJson, statsSnapshotJson = statsSnapshotJson, llmModel = llmModel, seenAt = seenAt,
)

fun FocusProfile.toEntity() = FocusProfileEntity(
    id = id, generatedAt = generatedAt, periodStart = periodStart, periodEnd = periodEnd, profileText = profileText,
    insightsJson = insightsJson, statsSnapshotJson = statsSnapshotJson, llmModel = llmModel, seenAt = seenAt,
)
