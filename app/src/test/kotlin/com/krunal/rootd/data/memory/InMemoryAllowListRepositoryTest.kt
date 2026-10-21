package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.AlwaysAllowedApp
import com.krunal.rootd.domain.model.AlwaysAllowedReason
import com.krunal.rootd.testing.FixedClock
import com.krunal.rootd.testing.aSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryAllowListRepositoryTest {

    private val sessions = InMemorySessionRepository()
    private val repo = InMemoryAllowListRepository(sessions, FixedClock())

    @Test
    fun startsWithOneDefaultList() = runTest {
        val lists = repo.observeAllowLists().first()
        assertEquals(1, lists.size)
        assertTrue(lists.single().isDefault)
        assertEquals(lists.single(), repo.getDefault())
    }

    @Test
    fun delete_lastList_returnsFalse() = runTest {
        assertFalse(repo.delete(repo.getDefault().id))
        assertEquals(1, repo.observeAllowLists().first().size)
    }

    @Test
    fun delete_inUseByOngoingSession_returnsFalse() = runTest {
        val work = repo.create("Work")
        sessions.insert(aSession(allowListId = work))
        assertFalse(repo.delete(work))
    }

    @Test
    fun delete_unusedList_returnsTrue() = runTest {
        val work = repo.create("Work")
        assertTrue(repo.delete(work))
        assertEquals(1, repo.observeAllowLists().first().size)
    }

    @Test
    fun setDefault_keepsExactlyOneDefault() = runTest {
        val work = repo.create("Work")
        repo.setDefault(work)
        val defaults = repo.observeAllowLists().first().filter { it.isDefault }
        assertEquals(listOf(work), defaults.map { it.id })
    }

    @Test
    fun setPackages_replacesPackages() = runTest {
        val id = repo.getDefault().id
        repo.setPackages(id, setOf("a", "b"))
        repo.setPackages(id, setOf("b", "c"))
        assertEquals(setOf("b", "c"), repo.packagesFor(id))
    }

    @Test
    fun removeAlwaysAllowed_systemDefault_returnsFalse() = runTest {
        repo.addAlwaysAllowed(AlwaysAllowedApp("com.android.dialer", AlwaysAllowedReason.PHONE, isSystemDefault = true))
        assertFalse(repo.removeAlwaysAllowed("com.android.dialer"))
        assertEquals(setOf("com.android.dialer"), repo.alwaysAllowedPackages())
    }

    @Test
    fun removeAlwaysAllowed_userApp_returnsTrue() = runTest {
        repo.addAlwaysAllowed(AlwaysAllowedApp("com.example.notes", AlwaysAllowedReason.USER, isSystemDefault = false))
        assertTrue(repo.removeAlwaysAllowed("com.example.notes"))
        assertEquals(emptySet<String>(), repo.alwaysAllowedPackages())
    }

    @Test
    fun addAlwaysAllowed_duplicate_isIgnored() = runTest {
        repo.addAlwaysAllowed(AlwaysAllowedApp("com.android.dialer", AlwaysAllowedReason.PHONE, isSystemDefault = true))
        repo.addAlwaysAllowed(AlwaysAllowedApp("com.android.dialer", AlwaysAllowedReason.USER, isSystemDefault = false))
        val apps = repo.observeAlwaysAllowed().first()
        assertEquals(1, apps.size)
        assertTrue(apps.single().isSystemDefault)
    }
}
