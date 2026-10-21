package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.krunal.rootd.domain.model.FailReason
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = SpeciesEntity::class,
            parentColumns = ["id"],
            childColumns = ["species_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AllowListEntity::class,
            parentColumns = ["id"],
            childColumns = ["allow_list_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["started_local_date"]),
        Index(value = ["started_at"]),
        Index(value = ["tag_id"]),
        Index(value = ["species_id"]),
        Index(value = ["allow_list_id"])
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "mode") val mode: SessionMode,
    @ColumnInfo(name = "planned_minutes") val plannedMinutes: Int?,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "started_local_date") val startedLocalDate: String,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "status") val status: SessionStatus = SessionStatus.ONGOING,
    @ColumnInfo(name = "fail_reason") val failReason: FailReason?,
    @ColumnInfo(name = "paused_seconds") val pausedSeconds: Long = 0,
    @ColumnInfo(name = "pause_started_at") val pauseStartedAt: Long?,
    @ColumnInfo(name = "focus_seconds") val focusSeconds: Long = 0,
    @ColumnInfo(name = "species_id") val speciesId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long?,
    @ColumnInfo(name = "allow_list_id") val allowListId: Long?,
    @ColumnInfo(name = "goal_note") val goalNote: String?,
    @ColumnInfo(name = "took_break") val tookBreak: Boolean = false,
    @ColumnInfo(name = "pass_count") val passCount: Int = 0,
    @ColumnInfo(name = "request_count") val requestCount: Int = 0,
    @ColumnInfo(name = "last_denied_at") val lastDeniedAt: Long?,
) {
    fun toModel(): com.krunal.rootd.domain.model.Session {
        return com.krunal.rootd.domain.model.Session(
            id = id,
            mode = mode,
            plannedMinutes = plannedMinutes,
            startedAt = Instant.ofEpochMilli(startedAt),
            startedLocalDate = LocalDate.parse(startedLocalDate),
            endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
            status = status,
            failReason = failReason,
            pausedSeconds = pausedSeconds,
            pauseStartedAt = pauseStartedAt?.let { Instant.ofEpochMilli(it) },
            focusSeconds = focusSeconds,
            speciesId = speciesId,
            tagId = tagId,
            allowListId = allowListId,
            goalNote = goalNote,
            tookBreak = tookBreak,
            passCount = passCount,
            requestCount = requestCount,
            lastDeniedAt = lastDeniedAt?.let { Instant.ofEpochMilli(it) }
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.Session): SessionEntity {
            return SessionEntity(
                id = model.id,
                mode = model.mode,
                plannedMinutes = model.plannedMinutes,
                startedAt = model.startedAt.toEpochMilli(),
                startedLocalDate = model.startedLocalDate.toString(),
                endedAt = model.endedAt?.toEpochMilli(),
                status = model.status,
                failReason = model.failReason,
                pausedSeconds = model.pausedSeconds,
                pauseStartedAt = model.pauseStartedAt?.toEpochMilli(),
                focusSeconds = model.focusSeconds,
                speciesId = model.speciesId,
                tagId = model.tagId,
                allowListId = model.allowListId,
                goalNote = model.goalNote,
                tookBreak = model.tookBreak,
                passCount = model.passCount,
                requestCount = model.requestCount,
                lastDeniedAt = model.lastDeniedAt?.toEpochMilli()
            )
        }
    }
}