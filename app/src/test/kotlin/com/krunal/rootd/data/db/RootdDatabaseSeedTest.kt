package com.krunal.rootd.data.db

import com.krunal.rootd.testing.FixedClock
import com.krunal.rootd.testing.T0
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RootdDatabaseSeedTest {

    private val clock = FixedClock(now = T0)
    private val db = newTestDb(clock)

    @After
    fun close() = db.close()

    @Test
    fun create_seedsSevenSpeciesWithStartersUnlocked() = runTest {
        val species = db.catalogDao().observeSpecies().first()
        assertEquals(
            listOf(
                Triple(1L, "oak", 0), Triple(2L, "pine", 0), Triple(3L, "birch", 300), Triple(4L, "maple", 500),
                Triple(5L, "cherry", 800), Triple(6L, "bamboo", 1000), Triple(7L, "baobab", 1500),
            ),
            species.map { Triple(it.id, it.key, it.priceCoins) },
        )
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), species.map { it.sortOrder })
        assertEquals(listOf(T0, T0), species.take(2).map { it.unlockedAt })
        assertTrue(species.drop(2).all { it.unlockedAt == null })
    }

    @Test
    fun create_seedsOneDefaultAllowList() = runTest {
        val lists = db.allowListDao().observeAllowLists().first()
        assertEquals(1, lists.size)
        assertEquals("Default", lists.single().name)
        assertTrue(lists.single().isDefault)
    }

    @Test
    fun create_seedsFourTags() = runTest {
        val tags = db.catalogDao().observeTags(includeArchived = true).first()
        assertEquals(listOf("study", "work", "read", "exercise"), tags.map { it.name })
        assertEquals(4, tags.map { it.colorArgb }.toSet().size) // distinct colors
        assertTrue(tags.none { it.archived })
    }

    @Test
    fun create_doesNotSeedAlwaysAllowedApps() = runTest {
        assertEquals(emptyList<Any>(), db.allowListDao().observeAlwaysAllowed().first())
    }

    @Test
    fun foreignKeys_areEnforced() {
        db.openHelper.readableDatabase.query("PRAGMA foreign_keys").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }

    @Test
    fun partialUniqueIndex_existsWithExactSql() {
        db.openHelper.readableDatabase.query(
            "SELECT sql FROM sqlite_master WHERE type = 'index' AND name = 'index_sessions_single_ongoing'",
        ).use { c ->
            assertTrue("partial index missing", c.moveToFirst())
            assertEquals(
                "CREATE UNIQUE INDEX index_sessions_single_ongoing ON sessions(status) WHERE status = 'ongoing'",
                c.getString(0).replace(Regex("\\s+"), " ").replace("IF NOT EXISTS ", ""),
            )
        }
    }

    @Test
    fun seedsRunOnlyOnce_reopeningDoesNotDuplicate() = runTest {
        db.catalogDao().observeSpecies().first()
        assertEquals(7, db.catalogDao().observeSpecies().first().size)
        assertNotNull(db.catalogDao().getSpecies(7))
        assertNull(db.catalogDao().getSpecies(8))
    }
}
