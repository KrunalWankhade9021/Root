package com.krunal.rootd.data.contract

import com.krunal.rootd.domain.model.PassEndReason
import com.krunal.rootd.domain.model.ReasonCategory
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.repo.PassRequestRepository
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.T0
import com.krunal.rootd.testing.aPassRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** PassRequestRepository contract (T01 interface comments). Run against every implementation. */
abstract class PassRequestRepositoryContract {

    protected abstract fun createRepo(): PassRequestRepository

    /** A session id that pass rows may reference (Room has a foreign key). */
    protected abstract suspend fun newSessionId(): Long
    protected val repo: PassRequestRepository by lazy { createRepo() }

    @Test
    fun insert_get_update_roundTripEveryField() = runTest {
        val sid = newSessionId()
        val original = aPassRequest(sessionId = sid).copy(reasonCategory = ReasonCategory.WORK_MESSAGE)
        val id = repo.insert(original)
        assertEquals(original.copy(id = id), repo.get(id))
        val ended = repo.get(id)!!.copy(
            passStartedAt = T0, passEndedAt = T0.plusSeconds(90), passEndReason = PassEndReason.RETURNED_TO_ROOTD,
            usedSeconds = 90, returnedBeforeExpiry = true,
        )
        repo.update(ended)
        assertEquals(ended, repo.get(id))
    }

    @Test
    fun forSession_orderedByAskedAt_onlyThatSession() = runTest {
        val a = newSessionId()
        val b = newSessionId()
        val late = repo.insert(aPassRequest(sessionId = a, askedAt = T0.plusSeconds(600)))
        val early = repo.insert(aPassRequest(sessionId = a, askedAt = T0.plusSeconds(60)))
        repo.insert(aPassRequest(sessionId = b))
        assertEquals(listOf(early, late), repo.forSession(a).map { it.id })
    }

    @Test
    fun activePass_startedButNotEnded() = runTest {
        val sid = newSessionId()
        val id = repo.insert(aPassRequest(sessionId = sid))
        assertNull(repo.activePass(sid))
        repo.update(repo.get(id)!!.copy(passStartedAt = T0))
        assertEquals(id, repo.activePass(sid)!!.id)
        repo.update(repo.get(id)!!.copy(passEndedAt = T0.plusSeconds(60)))
        assertNull(repo.activePass(sid))
    }

    @Test
    fun countNonAppealForAppOn_excludesAppealsOtherAppsOtherDays() = runTest {
        val sid = newSessionId()
        val pushBack = repo.insert(aPassRequest(sessionId = sid, verdict = Verdict.PUSH_BACK))
        repo.insert(aPassRequest(sessionId = sid, wasAppeal = true, originalRequestId = pushBack))
        repo.insert(aPassRequest(sessionId = sid))
        repo.insert(aPassRequest(sessionId = sid, askedLocalDate = D0.minusDays(1)))
        repo.insert(aPassRequest(sessionId = sid, appPackage = "com.other"))
        assertEquals(2, repo.countNonAppealForAppOn("com.example.social", D0))
    }

    @Test
    fun between_filtersByAskedLocalDate() = runTest {
        val sid = newSessionId()
        repo.insert(aPassRequest(sessionId = sid, askedLocalDate = D0.minusDays(8)))
        val inside = repo.insert(aPassRequest(sessionId = sid, askedLocalDate = D0.minusDays(2)))
        assertEquals(listOf(inside), repo.between(D0.minusDays(6), D0).map { it.id })
    }
}
