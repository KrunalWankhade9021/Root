package com.krunal.rootd.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.krunal.rootd.domain.model.PassEndReason
import com.krunal.rootd.domain.model.PassRequest
import com.krunal.rootd.domain.model.ReasonCategory
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.model.VerdictSource
import java.time.Instant
import java.time.LocalDate

/** data-model §4. */
@Entity(
    tableName = "pass_requests",
    foreignKeys = [
        ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PassRequestEntity::class, parentColumns = ["id"], childColumns = ["original_request_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("session_id", "asked_at"),
        Index("app_package", "asked_local_date"),
        Index("original_request_id"),
    ],
)
data class PassRequestEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "app_package") val appPackage: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "asked_at") val askedAt: Instant,
    @ColumnInfo(name = "asked_local_date") val askedLocalDate: LocalDate,
    @ColumnInfo(name = "reason_text") val reasonText: String,
    @ColumnInfo(name = "reason_category", defaultValue = "other") val reasonCategory: ReasonCategory = ReasonCategory.OTHER,
    @ColumnInfo(name = "was_appeal", defaultValue = "0") val wasAppeal: Boolean = false,
    @ColumnInfo(name = "original_request_id") val originalRequestId: Long? = null,
    @ColumnInfo(name = "verdict") val verdict: Verdict,
    @ColumnInfo(name = "verdict_source") val verdictSource: VerdictSource,
    @ColumnInfo(name = "llm_message") val llmMessage: String?,
    @ColumnInfo(name = "llm_model") val llmModel: String?,
    @ColumnInfo(name = "llm_latency_ms") val llmLatencyMs: Long?,
    @ColumnInfo(name = "granted_minutes", defaultValue = "0") val grantedMinutes: Int = 0,
    @ColumnInfo(name = "pass_started_at") val passStartedAt: Instant? = null,
    @ColumnInfo(name = "pass_ended_at") val passEndedAt: Instant? = null,
    @ColumnInfo(name = "pass_end_reason") val passEndReason: PassEndReason? = null,
    @ColumnInfo(name = "used_seconds", defaultValue = "0") val usedSeconds: Long = 0,
    @ColumnInfo(name = "returned_before_expiry") val returnedBeforeExpiry: Boolean? = null,
)

fun PassRequestEntity.toModel() = PassRequest(
    id = id, sessionId = sessionId, appPackage = appPackage, appLabel = appLabel, askedAt = askedAt,
    askedLocalDate = askedLocalDate, reasonText = reasonText, reasonCategory = reasonCategory, wasAppeal = wasAppeal,
    originalRequestId = originalRequestId, verdict = verdict, verdictSource = verdictSource, llmMessage = llmMessage,
    llmModel = llmModel, llmLatencyMs = llmLatencyMs, grantedMinutes = grantedMinutes, passStartedAt = passStartedAt,
    passEndedAt = passEndedAt, passEndReason = passEndReason, usedSeconds = usedSeconds,
    returnedBeforeExpiry = returnedBeforeExpiry,
)

fun PassRequest.toEntity() = PassRequestEntity(
    id = id, sessionId = sessionId, appPackage = appPackage, appLabel = appLabel, askedAt = askedAt,
    askedLocalDate = askedLocalDate, reasonText = reasonText, reasonCategory = reasonCategory, wasAppeal = wasAppeal,
    originalRequestId = originalRequestId, verdict = verdict, verdictSource = verdictSource, llmMessage = llmMessage,
    llmModel = llmModel, llmLatencyMs = llmLatencyMs, grantedMinutes = grantedMinutes, passStartedAt = passStartedAt,
    passEndedAt = passEndedAt, passEndReason = passEndReason, usedSeconds = usedSeconds,
    returnedBeforeExpiry = returnedBeforeExpiry,
)
