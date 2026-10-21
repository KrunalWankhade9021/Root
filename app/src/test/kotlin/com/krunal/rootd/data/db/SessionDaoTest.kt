package com.krunal.rootd.data.db

import android.database.sqlite.SQLiteConstraintException
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.T0
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionDaoTest {

    private val db = newTestDb()
    private val dao = db.sessionDao()

    @After
    fun close() = db.close()

    @Test
    fun insert_secondOngoing_violatesPartialUniqueIndex() = runTest {
        dao.insert(sessionRow())
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insert(sessionRow(startedAt = T0.plusSeconds(60))) }
        }
    }

    @Test
    fun update_secondSessionToOngoing_violatesPartialUniqueIndex() = runTest {
        dao.insert(sessionRow())
        val id = dao.insert(sessionRow(status = SessionStatus.GROWN))
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.update(dao.get(id)!!.copy(status = SessionStatus.ONGOING, endedAt = null)) }
        }
    }

    @Test
    fun twoFinishedSessions_allowed() = runTest {
        dao.insert(sessionRow(status = SessionStatus.GROWN))
        dao.insert(sessionRow(status = SessionStatus.WITHERED))
        dao.insert(sessionRow()) // plus one ongoing
        assertEquals(2, dao.countFinished())
    }

    @Test
    fun deleteTag_setsSessionTagNull() = runTest {
        val id = dao.insert(sessionRow(tagId = 1))
        db.catalogDao().deleteTag(1)
        assertNull(dao.get(id)!!.tagId)
    }

    @Test
    fun deleteAllowList_setsSessionAllowListNull() = runTest {
        val id = dao.insert(sessionRow(allowListId = 1))
        db.allowListDao().delete(1)
        assertNull(dao.get(id)!!.allowListId)
    }

    @Test
    fun getOngoing_andObserveOngoing() = runTest {
        assertNull(dao.getOngoing())
        val id = dao.insert(sessionRow())
        assertEquals(id, dao.getOngoing()!!.id)
        assertEquals(id, dao.observeOngoing().first()!!.id)
    }

    @Test
    fun observeBetween_filtersByLocalDate_ordersByStartedAt() = runTest {
        dao.insert(sessionRow(status = SessionStatus.GROWN, startedLocalDate = D0.minusDays(1)))
        val late = dao.insert(sessionRow(status = SessionStatus.GROWN, startedAt = T0.plusSeconds(7200)))
        val early = dao.insert(sessionRow(status = SessionStatus.GROWN, startedAt = T0.plusSeconds(60)))
        dao.insert(sessionRow(status = SessionStatus.GROWN, startedLocalDate = D0.plusDays(1)))
        assertEquals(listOf(early, late), dao.observeBetween(D0, D0).first().map { it.id })
    }

    @Test
    fun earliestStartDate() = runTest {
        assertNull(dao.earliestStartDate())
        dao.insert(sessionRow(status = SessionStatus.GROWN, startedLocalDate = D0.minusDays(3)))
        dao.insert(sessionRow(status = SessionStatus.GROWN, startedLocalDate = D0))
        assertEquals(D0.minusDays(3), dao.earliestStartDate())
    }

    @Test
    fun storageFormats_matchDataModelSection1() = runTest {
        dao.insert(sessionRow())
        db.openHelper.readableDatabase.query(
            "SELECT started_at, started_local_date, mode, status, took_break, paused_seconds FROM sessions",
        ).use { c ->
            c.moveToFirst()
            assertEquals(T0.toEpochMilli(), c.getLong(0))
            assertEquals("2026-10-21", c.getString(1))
            assertEquals("timer", c.getString(2))
            assertEquals("ongoing", c.getString(3))
            assertEquals(0, c.getInt(4))
            assertEquals(0, c.getInt(5))
        }
    }
}
