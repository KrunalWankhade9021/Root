package com.krunal.rootd.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.krunal.rootd.domain.model.FailReason
import com.krunal.rootd.domain.model.Session
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import java.time.Instant
import java.time.LocalDate

/** data-model §3. The "one ongoing session" partial unique index is created in RootdDatabase's callback (§8). */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(entity = SpeciesEntity::class, parentColumns = ["id"], childColumns = ["species_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = AllowListEntity::class, parentColumns = ["id"], childColumns = ["allow_list_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("started_local_date"),
        Index("started_at"),
        Index("tag_id"),
        Index("species_id"),
        Index("allow_list_id"),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "mode") val mode: SessionMode,
    @ColumnInfo(name = "planned_minutes") val plannedMinutes: Int?,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "started_local_date") val startedLocalDate: LocalDate,
    @ColumnInfo(name = "ended_at") val endedAt: Instant?,
    @ColumnInfo(name = "status", defaultValue = "ongoing") val status: SessionStatus,
    @ColumnInfo(name = "fail_reason") val failReason: FailReason?,
    @ColumnInfo(name = "paused_seconds", defaultValue = "0") val pausedSeconds: Long,
    @ColumnInfo(name = "pause_started_at") val pauseStartedAt: Instant?,
    @ColumnInfo(name = "focus_seconds", defaultValue = "0") val focusSeconds: Long,
    @ColumnInfo(name = "species_id") val speciesId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long?,
    @ColumnInfo(name = "allow_list_id") val allowListId: Long?,
    @ColumnInfo(name = "goal_note") val goalNote: String?,
    @ColumnInfo(name = "took_break", defaultValue = "0") val tookBreak: Boolean,
    @ColumnInfo(name = "pass_count", defaultValue = "0") val passCount: Int,
    @ColumnInfo(name = "request_count", defaultValue = "0") val requestCount: Int,
    @ColumnInfo(name = "last_denied_at") val lastDeniedAt: Instant?,
)

fun SessionEntity.toModel() = Session(
    id = id, mode = mode, plannedMinutes = plannedMinutes, startedAt = startedAt, startedLocalDate = startedLocalDate,
    endedAt = endedAt, status = status, failReason = failReason, pausedSeconds = pausedSeconds,
    pauseStartedAt = pauseStartedAt, focusSeconds = focusSeconds, speciesId = speciesId, tagId = tagId,
    allowListId = allowListId, goalNote = goalNote, tookBreak = tookBreak, passCount = passCount,
    requestCount = requestCount, lastDeniedAt = lastDeniedAt,
)

fun Session.toEntity() = SessionEntity(
    id = id, mode = mode, plannedMinutes = plannedMinutes, startedAt = startedAt, startedLocalDate = startedLocalDate,
    endedAt = endedAt, status = status, failReason = failReason, pausedSeconds = pausedSeconds,
    pauseStartedAt = pauseStartedAt, focusSeconds = focusSeconds, speciesId = speciesId, tagId = tagId,
    allowListId = allowListId, goalNote = goalNote, tookBreak = tookBreak, passCount = passCount,
    requestCount = requestCount, lastDeniedAt = lastDeniedAt,
)
