package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.T0
import com.krunal.rootd.testing.aSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class InMemorySessionRepositoryTest {

    private val repo = InMemorySessionRepository()

    @Test
    fun insert_secondOngoing_throws() = runTest {
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
    fun observeBetween_filtersByStartedLocalDate() = runTest {
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
}
