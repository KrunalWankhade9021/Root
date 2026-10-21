package com.krunal.rootd.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.krunal.rootd.data.db.entity.SpeciesEntity
import com.krunal.rootd.data.db.entity.TagEntity
import com.krunal.rootd.testing.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RootdDatabaseSeedTest {

    @Test
    fun create_seedsSevenSpeciesWithStartersUnlocked() = runBlockingTest {
        val db = RootdDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val species = db.catalogDao().observeSpecies().first()
        assertEquals(7, species.size)

        val starters = species.filter { it.unlockedAt != null }
        assertEquals(2, starters.size)
        assertEquals(setOf(1L, 2L), starters.map { it.id }.toSet())

        val oak = species.first { it.key == "oak" }
        assertEquals(0, oak.priceCoins)
        assertNotNull(oak.unlockedAt)

        val pine = species.first { it.key == "pine" }
        assertEquals(0, pine.priceCoins)
        assertNotNull(pine.unlockedAt)

        val birch = species.first { it.key == "birch" }
        assertEquals(300, birch.priceCoins)
        assertNull(birch.unlockedAt)

        val prices = species.map { it.key to it.priceCoins }
        assertEquals(
            listOf("oak" to 0, "pine" to 0, "birch" to 300, "maple" to 500, "cherry" to 800, "bamboo" to 1000, "baobab" to 1500),
            prices
        )

        val sortOrders = species.map { it.key to it.sortOrder }
        assertEquals(listOf("oak" to 0, "pine" to 1, "birch" to 2, "maple" to 3, "cherry" to 4, "bamboo" to 5, "baobab" to 6), sortOrders)
    }

    @Test
    fun create_seedsOneDefaultAllowList() = runBlockingTest {
        val db = RootdDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val lists = db.allowListDao().observeAllowLists().first()
        assertEquals(1, lists.size)
        assertEquals("Default", lists[0].name)
        assertEquals(true, lists[0].isDefault)
    }

    @Test
    fun create_seedsFourTags() = runBlockingTest {
        val db = RootdDatabase.inMemory(ApplicationProvider.getApplicationContext())
        val tags = db.catalogDao().observeTags().first()
        assertEquals(4, tags.size)
        assertEquals(listOf("study", "work", "read", "exercise"), tags.map { it.name })
    }
}