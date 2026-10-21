package com.krunal.rootd.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.krunal.rootd.data.db.entity.SessionEntity
import com.krunal.rootd.domain.model.FailReason
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.T0
import com.krunal.rootd.testing.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class SessionDaoTest {

    @Test
    fun insert_secondOngoing_violatesPartialUniqueIndex() = runBlockingTest {
        val db = RootdDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val dao = db.sessionDao()

        val session1 = SessionEntity(
            mode = SessionMode.TIMER,
            plannedMinutes = 25,
            startedAt = T0.toEpochMilli(),
            startedLocalDate = D0.toString(),
            status = SessionStatus.ONGOING,
            speciesId = 1
        )
        dao.insert(session1)

        val session2 = SessionEntity(
            mode = SessionMode.STOPWATCH,
            plannedMinutes = null,
            startedAt = T0.plusSeconds(60).toEpochMilli(),
            startedLocalDate = D0.toString(),
            status = SessionStatus.ONGOING,
            speciesId = 1
        )

        try {
            dao.insert(session2)
            fail("Expected SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            // Expected
        }
    }

    @Test
    fun twoFinishedSessions_allowed() = runBlockingTest {
        val db = RootdDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val dao = db.sessionDao()

        val session1 = SessionEntity(
            mode = SessionMode.TIMER,
            plannedMinutes = 25,
            startedAt = T0.toEpochMilli(),
            startedLocalDate = D0.toString(),
            endedAt = T0.plusSeconds(25 * 60).toEpochMilli(),
            status = SessionStatus.GROWN,
            speciesId = 1
        )
        dao.insert(session1)

        val session2 = SessionEntity(
            mode = SessionMode.TIMER,
            plannedMinutes = 30,
            startedAt = T0.plusSeconds(3600).toEpochMilli(),
            startedLocalDate = D0.toString(),
            endedAt = T0.plusSeconds(3600 + 30 * 60).toEpochMilli(),
            status = SessionStatus.GROWN,
            speciesId = 1
        )
        dao.insert(session2)

        val all = dao.observeBetween(D0.toString(), D0.toString()).first()
        assertEquals(2, all.size)
    }

    @Test
    fun deleteTag_setsSessionTagNull() = runBlockingTest {
        val db = RootdDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val sessionDao = db.sessionDao()
        val catalogDao = db.catalogDao()

        val tag = catalogDao.getTag(1) ?: fail("Tag 1 not found")
        val session = SessionEntity(
            mode = SessionMode.TIMER,
            plannedMinutes = 25,
            startedAt = T0.toEpochMilli(),
            startedLocalDate = D0.toString(),
            endedAt = T0.plusSeconds(25 * 60).toEpochMilli(),
            status = SessionStatus.GROWN,
            speciesId = 1,
            tagId = tag.id
        )
        sessionDao.insert(session)

        catalogDao.deleteTag(tag.id)

        val updated = sessionDao.get(session.id)
        assertNotNull(updated)
        assertNull(updated?.tagId)
    }

    @Test
    fun deleteAllowList_setsSessionAllowListNull() = runBlockingTest {
        val db = RootdDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val sessionDao = db.sessionDao()
        val allowListDao = db.allowListDao()

        val list = allowListDao.getDefault() ?: fail("Default list not found")
        val session = SessionEntity(
            mode = SessionMode.TIMER,
            plannedMinutes = 25,
            startedAt = T0.toEpochMilli(),
            startedLocalDate = D0.toString(),
            endedAt = T0.plusSeconds(25 * 60).toEpochMilli(),
            status = SessionStatus.GROWN,
            speciesId = 1,
            allowListId = list.id
        )
        sessionDao.insert(session)

        // Create another list to be the default, then delete the original
        val newListId = allowListDao.insertAllowList(
            com.krunal.rootd.data.db.entity.AllowListEntity(
                name = "New Default",
                isDefault = false,
                createdAt = T0.toEpochMilli()
            )
        )
        allowListDao.setDefault(newListId)
        allowListDao.deleteAllowList(list.id)

        val updated = sessionDao.get(session.id)
        assertNotNull(updated)
        assertNull(updated?.allowListId)
    }
}