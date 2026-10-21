package com.krunal.rootd.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.krunal.rootd.data.db.entity.PassRequestEntity
import com.krunal.rootd.data.db.entity.SessionEntity
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.model.VerdictSource
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.FixedClock
import com.krunal.rootd.testing.T0
import java.time.Instant
import java.time.LocalDate

fun newTestDb(clock: FixedClock = FixedClock()): RootdDatabase =
    RootdDatabase.inMemory(ApplicationProvider.getApplicationContext<Context>(), clock)

fun sessionRow(
    status: SessionStatus = SessionStatus.ONGOING,
    startedAt: Instant = T0,
    startedLocalDate: LocalDate = D0,
    tagId: Long? = null,
    allowListId: Long? = null,
    speciesId: Long = 1,
) = SessionEntity(
    mode = SessionMode.TIMER, plannedMinutes = 25, startedAt = startedAt, startedLocalDate = startedLocalDate,
    endedAt = if (status == SessionStatus.ONGOING) null else startedAt.plusSeconds(1500), status = status,
    failReason = null, pausedSeconds = 0, pauseStartedAt = null, focusSeconds = 0, speciesId = speciesId,
    tagId = tagId, allowListId = allowListId, goalNote = null, tookBreak = false, passCount = 0, requestCount = 0,
    lastDeniedAt = null,
)

fun passRow(
    sessionId: Long,
    appPackage: String = "com.example.social",
    askedLocalDate: LocalDate = D0,
    wasAppeal: Boolean = false,
    originalRequestId: Long? = null,
    verdict: Verdict = Verdict.ALLOW,
) = PassRequestEntity(
    sessionId = sessionId, appPackage = appPackage, appLabel = "Social", askedAt = T0, askedLocalDate = askedLocalDate,
    reasonText = "reason", wasAppeal = wasAppeal, originalRequestId = originalRequestId, verdict = verdict,
    verdictSource = VerdictSource.LLM, llmMessage = "ok", llmModel = "m", llmLatencyMs = 100,
    grantedMinutes = if (verdict == Verdict.ALLOW) 2 else 0,
)
