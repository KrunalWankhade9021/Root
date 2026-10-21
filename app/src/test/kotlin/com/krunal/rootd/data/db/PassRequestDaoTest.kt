package com.krunal.rootd.data.db

import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.T0
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PassRequestDaoTest {

    private val db = newTestDb()
    private val dao = db.passRequestDao()

    @After
    fun close() = db.close()

    @Test
    fun deleteSession_cascadesPassRequests() = runTest {
        val sessionId = db.sessionDao().insert(sessionRow())
        val pushBack = dao.insert(passRow(sessionId, verdict = Verdict.PUSH_BACK))
        dao.insert(passRow(sessionId, wasAppeal = true, originalRequestId = pushBack))
        db.sessionDao().delete(db.sessionDao().get(sessionId)!!)
        assertEquals(emptyList<Any>(), dao.forSession(sessionId))
    }

    @Test
    fun countNonAppealForAppOn_ignoresAppealsAndOtherDates() = runTest {
        val sessionId = db.sessionDao().insert(sessionRow())
        val first = dao.insert(passRow(sessionId, verdict = Verdict.PUSH_BACK))
        dao.insert(passRow(sessionId, wasAppeal = true, originalRequestId = first)) // appeal: not counted
        dao.insert(passRow(sessionId)) // counted
        dao.insert(passRow(sessionId, askedLocalDate = D0.minusDays(1))) // other day
        dao.insert(passRow(sessionId, appPackage = "com.other")) // other app
        assertEquals(2, dao.countNonAppealForAppOn("com.example.social", D0))
    }

    @Test
    fun forSession_orderedByAskedAt() = runTest {
        val sessionId = db.sessionDao().insert(sessionRow())
        val later = dao.insert(passRow(sessionId).copy(askedAt = T0.plusSeconds(600)))
        val earlier = dao.insert(passRow(sessionId).copy(askedAt = T0.plusSeconds(60)))
        assertEquals(listOf(earlier, later), dao.forSession(sessionId).map { it.id })
    }

    @Test
    fun activePass_isStartedButNotEnded() = runTest {
        val sessionId = db.sessionDao().insert(sessionRow())
        assertNull(dao.activePass(sessionId))
        val id = dao.insert(passRow(sessionId))
        assertNull(dao.activePass(sessionId)) // allowed but not started
        dao.update(dao.get(id)!!.copy(passStartedAt = T0))
        assertEquals(id, dao.activePass(sessionId)!!.id)
        dao.update(dao.get(id)!!.copy(passEndedAt = T0.plusSeconds(60)))
        assertNull(dao.activePass(sessionId))
    }

    @Test
    fun between_filtersByAskedLocalDate() = runTest {
        val sessionId = db.sessionDao().insert(sessionRow(status = SessionStatus.GROWN))
        dao.insert(passRow(sessionId, askedLocalDate = D0.minusDays(8)))
        val inside = dao.insert(passRow(sessionId, askedLocalDate = D0.minusDays(3)))
        assertEquals(listOf(inside), dao.between(D0.minusDays(6), D0).map { it.id })
    }
}
