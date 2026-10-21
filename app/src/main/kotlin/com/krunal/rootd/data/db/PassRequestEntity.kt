package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.krunal.rootd.domain.model.PassEndReason
import com.krunal.rootd.domain.model.ReasonCategory
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.model.VerdictSource
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "pass_requests",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PassRequestEntity::class,
            parentColumns = ["id"],
            childColumns = ["original_request_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id", "asked_at"]),
        Index(value = ["app_package", "asked_local_date"]),
        Index(value = ["original_request_id"])
    ]
)
data class PassRequestEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "app_package") val appPackage: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "asked_at") val askedAt: Long,
    @ColumnInfo(name = "asked_local_date") val askedLocalDate: String,
    @ColumnInfo(name = "reason_text") val reasonText: String,
    @ColumnInfo(name = "reason_category") val reasonCategory: ReasonCategory = ReasonCategory.OTHER,
    @ColumnInfo(name = "was_appeal") val wasAppeal: Boolean = false,
    @ColumnInfo(name = "original_request_id") val originalRequestId: Long?,
    @ColumnInfo(name = "verdict") val verdict: Verdict,
    @ColumnInfo(name = "verdict_source") val verdictSource: VerdictSource,
    @ColumnInfo(name = "llm_message") val llmMessage: String?,
    @ColumnInfo(name = "llm_model") val llmModel: String?,
    @ColumnInfo(name = "llm_latency_ms") val llmLatencyMs: Long?,
    @ColumnInfo(name = "granted_minutes") val grantedMinutes: Int = 0,
    @ColumnInfo(name = "pass_started_at") val passStartedAt: Long?,
    @ColumnInfo(name = "pass_ended_at") val passEndedAt: Long?,
    @ColumnInfo(name = "pass_end_reason") val passEndReason: PassEndReason?,
    @ColumnInfo(name = "used_seconds") val usedSeconds: Long = 0,
    @ColumnInfo(name = "returned_before_expiry") val returnedBeforeExpiry: Boolean?,
) {
    fun toModel(): com.krunal.rootd.domain.model.PassRequest {
        return com.krunal.rootd.domain.model.PassRequest(
            id = id,
            sessionId = sessionId,
            appPackage = appPackage,
            appLabel = appLabel,
            askedAt = Instant.ofEpochMilli(askedAt),
            askedLocalDate = LocalDate.parse(askedLocalDate),
            reasonText = reasonText,
            reasonCategory = reasonCategory,
            wasAppeal = wasAppeal,
            originalRequestId = originalRequestId,
            verdict = verdict,
            verdictSource = verdictSource,
            llmMessage = llmMessage,
            llmModel = llmModel,
            llmLatencyMs = llmLatencyMs,
            grantedMinutes = grantedMinutes,
            passStartedAt = passStartedAt?.let { Instant.ofEpochMilli(it) },
            passEndedAt = passEndedAt?.let { Instant.ofEpochMilli(it) },
            passEndReason = passEndReason,
            usedSeconds = usedSeconds,
            returnedBeforeExpiry = returnedBeforeExpiry
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.PassRequest): PassRequestEntity {
            return PassRequestEntity(
                id = model.id,
                sessionId = model.sessionId,
                appPackage = model.appPackage,
                appLabel = model.appLabel,
                askedAt = model.askedAt.toEpochMilli(),
                askedLocalDate = model.askedLocalDate.toString(),
                reasonText = model.reasonText,
                reasonCategory = model.reasonCategory,
                wasAppeal = model.wasAppeal,
                originalRequestId = model.originalRequestId,
                verdict = model.verdict,
                verdictSource = model.verdictSource,
                llmMessage = model.llmMessage,
                llmModel = model.llmModel,
                llmLatencyMs = model.llmLatencyMs,
                grantedMinutes = model.grantedMinutes,
                passStartedAt = model.passStartedAt?.toEpochMilli(),
                passEndedAt = model.passEndedAt?.toEpochMilli(),
                passEndReason = model.passEndReason,
                usedSeconds = model.usedSeconds,
                returnedBeforeExpiry = model.returnedBeforeExpiry
            )
        }
    }
}