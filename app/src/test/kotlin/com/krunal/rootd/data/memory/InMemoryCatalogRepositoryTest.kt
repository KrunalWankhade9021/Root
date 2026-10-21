package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.Tag
import com.krunal.rootd.testing.FixedClock
import com.krunal.rootd.testing.T0
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

class InMemoryCatalogRepositoryTest {

    private val clock = FixedClock()
    private val repo = InMemoryCatalogRepository(clock)

    @Test
    fun seeds_matchDataModelSection5() = runTest {
        assertEquals(listOf("study", "work", "read", "exercise"), repo.observeTags().first().map { it.name })
        val species = repo.observeSpecies().first()
        assertEquals(
            listOf("oak" to 0, "pine" to 0, "birch" to 300, "maple" to 500, "cherry" to 800, "bamboo" to 1000, "baobab" to 1500),
            species.map { it.key to it.priceCoins },
        )
        assertEquals(listOf(1L, 2L), species.filter { it.unlockedAt != null }.map { it.id })
    }

    @Test
    fun upsertTag_duplicateActiveNameIgnoringCase_throws() = runTest {
        try {
            repo.upsertTag(Tag(name = "STUDY", colorArgb = 0xFF000000.toInt(), createdAt = T0))
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun upsertTag_nameOfArchivedTag_allowed() = runTest {
        val study = repo.observeTags().first().first { it.name == "study" }
        repo.setTagArchived(study.id, true)
        val id = repo.upsertTag(Tag(name = "Study", colorArgb = 0xFF000000.toInt(), createdAt = T0))
        assertNotNull(repo.getTag(id))
    }

    @Test
    fun observeTags_hidesArchivedUnlessAsked() = runTest {
        val read = repo.observeTags().first().first { it.name == "read" }
        repo.setTagArchived(read.id, true)
        assertEquals(listOf("study", "work", "exercise"), repo.observeTags().first().map { it.name })
        assertEquals(4, repo.observeTags(includeArchived = true).first().size)
    }

    @Test
    fun upsertTag_existingId_updatesInPlace() = runTest {
        val work = repo.observeTags().first().first { it.name == "work" }
        repo.upsertTag(work.copy(name = "deep work"))
        assertEquals("deep work", repo.getTag(work.id)?.name)
        assertEquals(4, repo.observeTags().first().size)
    }

    @Test
    fun markSpeciesUnlocked_setsUnlockedAt() = runTest {
        repo.markSpeciesUnlocked(3, T0)
        assertEquals(T0, repo.getSpecies(3)?.unlockedAt)
    }
}
