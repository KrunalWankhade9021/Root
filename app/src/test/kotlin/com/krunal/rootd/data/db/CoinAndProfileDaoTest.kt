package com.krunal.rootd.data.db

import com.krunal.rootd.data.db.entity.CoinTransactionEntity
import com.krunal.rootd.data.db.entity.FocusProfileEntity
import com.krunal.rootd.domain.model.CoinKind
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.testing.D0
import com.krunal.rootd.testing.T0
import kotlinx.coroutines.flow.first
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
class CoinAndProfileDaoTest {

    private val db = newTestDb()
    private val coins = db.coinDao()
    private val profiles = db.focusProfileDao()

    @After
    fun close() = db.close()

    private fun tx(amount: Int, kind: CoinKind, sessionId: Long? = null, speciesId: Long? = null) =
        CoinTransactionEntity(at = T0, amount = amount, kind = kind, sessionId = sessionId, speciesId = speciesId, note = null)

    @Test
    fun sum_emptyLedger_isNull() = runTest {
        assertNull(coins.sum())
        assertNull(coins.observeSum().first())
    }

    @Test
    fun sum_mixedAmounts() = runTest {
        val s = db.sessionDao().insert(sessionRow(status = SessionStatus.GROWN))
        coins.insert(tx(21, CoinKind.SESSION_REWARD, sessionId = s))
        coins.insert(tx(4, CoinKind.NO_PASS_BONUS, sessionId = s))
        coins.insert(tx(-300, CoinKind.SPECIES_UNLOCK, speciesId = 3))
        assertEquals(-275, coins.sum())
        assertEquals(-275, coins.observeSum().first())
    }

    @Test
    fun forSession_returnsOnlyThatSessionsRows() = runTest {
        val a = db.sessionDao().insert(sessionRow(status = SessionStatus.GROWN))
        val b = db.sessionDao().insert(sessionRow(status = SessionStatus.GROWN))
        coins.insert(tx(9, CoinKind.SESSION_REWARD, sessionId = a))
        coins.insert(tx(3, CoinKind.SESSION_REWARD, sessionId = b))
        assertEquals(listOf(9), coins.forSession(a).map { it.amount })
    }

    @Test
    fun observeAll_newestFirst() = runTest {
        val first = coins.insert(tx(5, CoinKind.SESSION_REWARD))
        val second = coins.insert(tx(7, CoinKind.SESSION_REWARD))
        assertEquals(listOf(second, first), coins.observeAll().first().map { it.id })
    }

    @Test
    fun deleteSession_keepsCoinRowsWithNullSession() = runTest {
        val s = db.sessionDao().insert(sessionRow(status = SessionStatus.GROWN))
        coins.insert(tx(9, CoinKind.SESSION_REWARD, sessionId = s))
        db.sessionDao().delete(db.sessionDao().get(s)!!)
        assertEquals(listOf<Long?>(null), coins.observeAll().first().map { it.sessionId })
        assertEquals(9, coins.sum())
    }

    private fun profile(generatedAt: java.time.Instant) = FocusProfileEntity(
        generatedAt = generatedAt, periodStart = D0.minusDays(7), periodEnd = D0.minusDays(1), profileText = "p",
        insightsJson = "{}", statsSnapshotJson = "{}", llmModel = "m", seenAt = null,
    )

    @Test
    fun latestProfile_byGeneratedAt_andMarkSeen() = runTest {
        assertNull(profiles.latest())
        profiles.insert(profile(T0))
        val newer = profiles.insert(profile(T0.plusSeconds(3600)))
        assertEquals(newer, profiles.latest()!!.id)
        assertEquals(newer, profiles.observeLatest().first()!!.id)
        profiles.markSeen(newer, T0.plusSeconds(7200))
        assertEquals(T0.plusSeconds(7200), profiles.latest()!!.seenAt)
    }
}
