package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.CoinKind
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.repo.UnlockResult
import com.krunal.rootd.testing.FixedClock
import com.krunal.rootd.testing.T0
import com.krunal.rootd.testing.aPassRequest
import com.krunal.rootd.testing.aSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryCoinLedgerTest {

    private val clock = FixedClock()
    private val sessions = InMemorySessionRepository()
    private val passes = InMemoryPassRequestRepository()
    private val catalog = InMemoryCatalogRepository(clock)
    private val ledger = InMemoryCoinLedger(sessions, passes, catalog, clock)

    @Test
    fun settle_grown25min_noPass_rewardPlusBonus() = runTest {
        val id = sessions.insert(aSession(status = SessionStatus.GROWN, focusSeconds = 25 * 60))
        ledger.settleSession(id)
        assertEquals(listOf(CoinKind.SESSION_REWARD to 9, CoinKind.NO_PASS_BONUS to 1),
            ledger.forSession(id).map { it.kind to it.amount })
        assertEquals(10, ledger.balance())
    }

    @Test
    fun settleSession_isIdempotent() = runTest {
        val id = sessions.insert(aSession(status = SessionStatus.GROWN, focusSeconds = 60 * 60))
        ledger.settleSession(id)
        ledger.settleSession(id)
        assertEquals(2, ledger.forSession(id).size)
    }

    @Test
    fun settle_cancelled_writesNothing() = runTest {
        val id = sessions.insert(aSession(status = SessionStatus.CANCELLED))
        ledger.settleSession(id)
        assertEquals(0, ledger.forSession(id).size)
    }

    @Test
    fun settle_passCostExceedsBalance_isClamped() = runTest {
        val id = sessions.insert(aSession(status = SessionStatus.WITHERED, passCount = 1))
        passes.insert(aPassRequest(sessionId = id, passStartedAt = T0, passEndedAt = T0.plusSeconds(90), usedSeconds = 90))
        ledger.settleSession(id) // withered: no reward; pass cost 2*ceil(90/60)=4 but balance is 0 → no row
        assertEquals(0, ledger.balance())
        assertEquals(0, ledger.forSession(id).size)
    }

    @Test
    fun settle_zeroBalanceWithered_secondCallLaterDoesNotCharge() = runTest {
        val withered = sessions.insert(aSession(status = SessionStatus.WITHERED, passCount = 1))
        passes.insert(aPassRequest(sessionId = withered, passStartedAt = T0, passEndedAt = T0.plusSeconds(60), usedSeconds = 60))
        ledger.settleSession(withered) // balance 0 → nothing written
        val grown = sessions.insert(aSession(status = SessionStatus.GROWN, focusSeconds = 25 * 60))
        ledger.settleSession(grown) // +9 +1
        ledger.settleSession(withered) // must stay a no-op
        assertEquals(10, ledger.balance())
    }

    @Test
    fun unlock_notEnoughCoins_noRows() = runTest {
        assertEquals(UnlockResult.NOT_ENOUGH_COINS, ledger.unlockSpecies(3))
        assertEquals(0, ledger.balance())
    }

    @Test
    fun unlock_starterSpecies_alreadyUnlocked() = runTest {
        assertEquals(UnlockResult.ALREADY_UNLOCKED, ledger.unlockSpecies(1))
    }
}
