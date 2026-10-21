package com.krunal.rootd.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import com.krunal.rootd.data.db.entity.AllowListEntity
import com.krunal.rootd.data.db.entity.TagEntity
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.testing.FixedClock
import com.krunal.rootd.testing.T0
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Review findings on PR #5: real reopen of a file database, integrity rules and untested queries. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DbIntegrityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedClock(now = T0)
    private val opened = mutableListOf<RootdDatabase>()

    private fun fileDb(): RootdDatabase = RootdDatabase.create(context, clock).also { opened += it }

    private fun triggerNames(db: RootdDatabase): List<String> =
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'trigger' ORDER BY name").use { c ->
            generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList()
        }

    @After
    fun cleanUp() {
        opened.forEach { it.close() }
        context.deleteDatabase(RootdDatabase.FILE_NAME)
    }

    @Test
    fun fileDb_reopen_recreatesMissingTriggers_andDoesNotReseed() = runTest {
        context.deleteDatabase(RootdDatabase.FILE_NAME)
        val first = fileDb()
        assertEquals(7, first.catalogDao().observeSpecies().first().size)
        first.openHelper.writableDatabase.execSQL("DROP TRIGGER trg_sessions_single_ongoing_insert")
        first.openHelper.writableDatabase.execSQL("DROP TRIGGER trg_sessions_single_ongoing_update")
        first.close()

        val second = fileDb()
        assertEquals(listOf("trg_sessions_single_ongoing_insert", "trg_sessions_single_ongoing_update"), triggerNames(second))
        assertEquals(7, second.catalogDao().observeSpecies().first().size)
        assertEquals(4, second.catalogDao().observeTags(includeArchived = true).first().size)
    }

    @Test
    fun fileDb_reopenWithTwoOngoingRowsAlready_stillOpens() = runTest { // bad data must not brick the app
        context.deleteDatabase(RootdDatabase.FILE_NAME)
        val first = fileDb()
        first.catalogDao().observeSpecies().first()
        val w = first.openHelper.writableDatabase
        w.execSQL("DROP TRIGGER trg_sessions_single_ongoing_insert")
        repeat(2) {
            w.execSQL(
                "INSERT INTO sessions (mode, planned_minutes, started_at, started_local_date, status, species_id) " +
                    "VALUES ('timer', 25, ?, '2026-10-21', 'ongoing', 1)",
                arrayOf<Any?>(T0.toEpochMilli()),
            )
        }
        first.close()

        val second = fileDb()
        assertEquals(2, second.triggerNamesCount())
    }

    private fun RootdDatabase.triggerNamesCount() = triggerNames(this).size

    @Test
    fun setDefault_unknownId_keepsCurrentDefault() = runTest {
        val db = newTestDb(clock).also { opened += it }
        val dao = db.allowListDao()
        dao.setDefault(999)
        assertEquals("Default", dao.getDefault()!!.name)
    }

    @Test
    fun setDefault_switchesTheSingleDefault() = runTest {
        val db = newTestDb(clock).also { opened += it }
        val dao = db.allowListDao()
        val work = dao.insert(AllowListEntity(name = "Work", createdAt = T0))
        dao.setDefault(work)
        assertEquals(listOf(work), dao.observeAllowLists().first().filter { it.isDefault }.map { it.id })
    }

    @Test
    fun setPackages_replacesTheList() = runTest {
        val db = newTestDb(clock).also { opened += it }
        val dao = db.allowListDao()
        dao.setPackages(1, setOf("a", "b"))
        dao.setPackages(1, setOf("b", "c"))
        assertEquals(setOf("b", "c"), dao.packagesFor(1).toSet())
    }

    @Test
    fun allowListFindByName_caseInsensitive_excludesSelf() = runTest {
        val db = newTestDb(clock).also { opened += it }
        val dao = db.allowListDao()
        assertEquals(1L, dao.findByName("DEFAULT", excludeId = 0)!!.id)
        assertNull(dao.findByName("default", excludeId = 1))
    }

    @Test
    fun findActiveTagByName_caseInsensitive_ignoresArchived_excludesSelf() = runTest {
        val db = newTestDb(clock).also { opened += it }
        val dao = db.catalogDao()
        assertEquals(1L, dao.findActiveTagByName("STUDY", excludeId = 0)!!.id)
        assertNull(dao.findActiveTagByName("study", excludeId = 1))
        dao.setTagArchived(1, true)
        assertNull(dao.findActiveTagByName("study", excludeId = 0))
        val id = dao.insertTag(TagEntity(name = "Study", colorArgb = 1, createdAt = T0)) // archived name reusable
        assertEquals(id, dao.findActiveTagByName("study", excludeId = 0)!!.id)
    }

    @Test
    fun deleteSpeciesUsedBySession_isRestricted() = runTest {
        val db = newTestDb(clock).also { opened += it }
        db.sessionDao().insert(sessionRow(status = SessionStatus.GROWN, speciesId = 3))
        assertThrows(SQLiteConstraintException::class.java) {
            db.openHelper.writableDatabase.execSQL("DELETE FROM species WHERE id = 3")
        }
    }

    @Test
    fun deletePushBack_cascadesItsAppeal() = runTest {
        val db = newTestDb(clock).also { opened += it }
        val sessionId = db.sessionDao().insert(sessionRow())
        val dao = db.passRequestDao()
        val pushBack = dao.insert(passRow(sessionId, verdict = Verdict.PUSH_BACK))
        dao.insert(passRow(sessionId, wasAppeal = true, originalRequestId = pushBack))
        db.openHelper.writableDatabase.execSQL("DELETE FROM pass_requests WHERE id = ?", arrayOf<Any?>(pushBack))
        assertTrue(dao.forSession(sessionId).isEmpty())
    }
}
