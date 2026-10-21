package com.krunal.rootd.data.contract

import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.repo.SessionRepository
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.T0
import com.krunal.rootd.testing.aSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/** SessionRepository contract (T01 interface comments). Run against every implementation. */
abstract class SessionRepositoryContract {

    protected abstract fun createRepo(): SessionRepository
    protected val repo: SessionRepository by lazy { createRepo() }

    @Test
    fun insert_secondOngoing_throwsIllegalState() = runTest {
        repo.insert(aSession())
        try {
            repo.insert(aSession(startedAt = T0.plusSeconds(60)))
            fail("expected IllegalStateException")
        } catch (expected: IllegalStateException) {
        }
    }

    @Test
    fun insert_afterFinishingOngoing_allowed() = runTest {
        val id = repo.insert(aSession())
        repo.update(repo.get(id)!!.copy(status = SessionStatus.GROWN, endedAt = T0.plusSeconds(1500)))
        repo.insert(aSession(startedAt = T0.plusSeconds(3600)))
        assertEquals(2, repo.observeBetween(D0, D0).first().size)
    }

    @Test
    fun observeBetween_filtersByStartedLocalDate_ordersByStartedAt() = runTest {
        repo.insert(aSession(status = SessionStatus.GROWN, startedAt = T0, startedLocalDate = D0.minusDays(1)))
        val b = repo.insert(aSession(status = SessionStatus.GROWN, startedAt = T0.plusSeconds(7200), startedLocalDate = D0))
        val a = repo.insert(aSession(status = SessionStatus.GROWN, startedAt = T0.plusSeconds(60), startedLocalDate = D0))
        repo.insert(aSession(status = SessionStatus.GROWN, startedAt = T0, startedLocalDate = D0.plusDays(1)))

        val result = repo.observeBetween(D0, D0).first()

        assertEquals(listOf(a, b), result.map { it.id }) // ordered by startedAt
    }

    @Test
    fun observeOngoing_tracksOngoingSession() = runTest {
        assertNull(repo.observeOngoing().first())
        val id = repo.insert(aSession())
        assertEquals(id, repo.observeOngoing().first()?.id)
        repo.update(repo.get(id)!!.copy(status = SessionStatus.CANCELLED, endedAt = T0.plusSeconds(5)))
        assertNull(repo.observeOngoing().first())
    }

    @Test
    fun countFinished_countsGrownAndWitheredOnly() = runTest {
        repo.insert(aSession(status = SessionStatus.GROWN))
        repo.insert(aSession(status = SessionStatus.WITHERED))
        repo.insert(aSession(status = SessionStatus.CANCELLED))
        repo.insert(aSession())
        assertEquals(2, repo.countFinished())
    }

    @Test
    fun update_secondSessionToOngoing_throwsIllegalState() = runTest {
        repo.insert(aSession())
        val id = repo.insert(aSession(status = SessionStatus.GROWN))
        try {
            repo.update(repo.get(id)!!.copy(status = SessionStatus.ONGOING, endedAt = null))
            fail("expected IllegalStateException")
        } catch (expected: IllegalStateException) {
        }
    }

    @Test
    fun earliestStartDate_nullWhenEmpty_thenMinimum() = runTest {
        assertNull(repo.earliestStartDate())
        repo.insert(aSession(status = SessionStatus.GROWN, startedLocalDate = D0.minusDays(4)))
        repo.insert(aSession(status = SessionStatus.GROWN, startedLocalDate = D0))
        assertEquals(D0.minusDays(4), repo.earliestStartDate())
    }

    @Test
    fun insert_returnsIdAndRoundTripsEveryField() = runTest {
        val s = aSession(status = SessionStatus.WITHERED, tagId = 1, allowListId = 1, goalNote = "notes", passCount = 1)
            .copy(failReason = com.krunal.rootd.domain.model.FailReason.GAVE_UP, pausedSeconds = 30, focusSeconds = 600,
                requestCount = 2, lastDeniedAt = T0.plusSeconds(120))
        val id = repo.insert(s)
        assertEquals(s.copy(id = id), repo.get(id))
    }
}
