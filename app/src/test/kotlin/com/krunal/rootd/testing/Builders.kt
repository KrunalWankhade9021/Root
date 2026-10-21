package com.krunal.rootd.testing

import com.krunal.rootd.domain.model.PassRequest
import com.krunal.rootd.domain.model.Session
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.model.VerdictSource
import java.time.Instant
import java.time.LocalDate

val T0: Instant = Instant.parse("2026-10-21T09:00:00Z")
val D0: LocalDate = LocalDate.parse("2026-10-21")

fun aSession(
    id: Long = 0,
    mode: SessionMode = SessionMode.TIMER,
    plannedMinutes: Int? = if (mode == SessionMode.TIMER) 25 else null,
    startedAt: Instant = T0,
    startedLocalDate: LocalDate = D0,
    status: SessionStatus = SessionStatus.ONGOING,
    endedAt: Instant? = if (status == SessionStatus.ONGOING) null else startedAt.plusSeconds(25 * 60),
    focusSeconds: Long = 0,
    speciesId: Long = 1,
    tagId: Long? = null,
    allowListId: Long? = null,
    goalNote: String? = null,
    passCount: Int = 0,
): Session = Session(
    id = id, mode = mode, plannedMinutes = plannedMinutes, startedAt = startedAt,
    startedLocalDate = startedLocalDate, endedAt = endedAt, status = status, focusSeconds = focusSeconds,
    speciesId = speciesId, tagId = tagId, allowListId = allowListId, goalNote = goalNote,
    passCount = passCount, tookBreak = passCount > 0,
)

fun aPassRequest(
    id: Long = 0,
    sessionId: Long = 1,
    appPackage: String = "com.example.social",
    appLabel: String = "Social",
    askedAt: Instant = T0.plusSeconds(5 * 60),
    askedLocalDate: LocalDate = D0,
    reasonText: String = "reply to a work message",
    verdict: Verdict = Verdict.ALLOW,
    verdictSource: VerdictSource = VerdictSource.LLM,
    grantedMinutes: Int = if (verdict == Verdict.ALLOW) 2 else 0,
    wasAppeal: Boolean = false,
    originalRequestId: Long? = null,
    passStartedAt: Instant? = null,
    passEndedAt: Instant? = null,
    usedSeconds: Long = 0,
): PassRequest = PassRequest(
    id = id, sessionId = sessionId, appPackage = appPackage, appLabel = appLabel, askedAt = askedAt,
    askedLocalDate = askedLocalDate, reasonText = reasonText, verdict = verdict, verdictSource = verdictSource,
    llmMessage = "ok", llmModel = if (verdictSource == VerdictSource.LLM) "test-model" else null,
    llmLatencyMs = if (verdictSource == VerdictSource.LLM) 500 else null, grantedMinutes = grantedMinutes,
    wasAppeal = wasAppeal, originalRequestId = originalRequestId, passStartedAt = passStartedAt,
    passEndedAt = passEndedAt, usedSeconds = usedSeconds,
)
