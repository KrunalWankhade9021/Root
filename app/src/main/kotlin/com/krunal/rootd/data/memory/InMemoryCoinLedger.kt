package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.CoinKind
import com.krunal.rootd.domain.model.CoinTransaction
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.repo.CatalogRepository
import com.krunal.rootd.domain.repo.CoinLedger
import com.krunal.rootd.domain.repo.PassRequestRepository
import com.krunal.rootd.domain.repo.SessionRepository
import com.krunal.rootd.domain.repo.UnlockResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock

/**
 * In-memory ledger following data-model §6. T05 owns the real `RoomCoinLedger` and `CoinFormula`;
 * when it lands this class should delegate its maths to `CoinFormula` so the two can't drift.
 */
class InMemoryCoinLedger(
    private val sessions: SessionRepository,
    private val passes: PassRequestRepository,
    private val catalog: CatalogRepository,
    private val clock: Clock,
) : CoinLedger {
    private val lock = Mutex()
    private val rows = MutableStateFlow<List<CoinTransaction>>(emptyList())
    private val settled = mutableSetOf<Long>()
    private var nextId = 1L

    override fun observeBalance(): Flow<Int> = rows.map { all -> all.sumOf { it.amount } }

    override suspend fun balance(): Int = rows.value.sumOf { it.amount }

    override suspend fun settleSession(sessionId: Long) = lock.withLock {
        val session = sessions.get(sessionId) ?: return@withLock
        if (session.status != SessionStatus.GROWN && session.status != SessionStatus.WITHERED) return@withLock
        if (!settled.add(sessionId)) return@withLock // already settled (even if it produced no rows)

        if (session.status == SessionStatus.GROWN) {
            val m = (session.focusSeconds / 60).toInt()
            val base = (if (m < 25) 1 else 4) + m / 5 + (maxOf(m - 30, 0) / 30) * 5
            write(base, CoinKind.SESSION_REWARD, sessionId, note = null)
            val bonus = base / 5 // floor(base * 0.2)
            if (session.passCount == 0 && bonus > 0) write(bonus, CoinKind.NO_PASS_BONUS, sessionId, note = null)
        }

        val cost = 2 * passes.forSession(sessionId)
            .filter { it.passStartedAt != null }
            .sumOf { ((it.usedSeconds + 59) / 60).toInt() }
        if (cost > 0) {
            val balance = balance()
            when {
                balance == 0 -> Unit
                cost > balance -> write(-balance, CoinKind.PASS_COST, sessionId, note = "clamped from -$cost to -$balance")
                else -> write(-cost, CoinKind.PASS_COST, sessionId, note = null)
            }
        }
    }

    override suspend fun unlockSpecies(speciesId: Long): UnlockResult = lock.withLock {
        val species = catalog.getSpecies(speciesId) ?: throw IllegalArgumentException("No species $speciesId")
        if (species.unlockedAt != null) return@withLock UnlockResult.ALREADY_UNLOCKED
        if (balance() < species.priceCoins) return@withLock UnlockResult.NOT_ENOUGH_COINS
        write(-species.priceCoins, CoinKind.SPECIES_UNLOCK, sessionId = null, note = null, speciesId = speciesId)
        catalog.markSpeciesUnlocked(speciesId, clock.instant())
        UnlockResult.UNLOCKED
    }

    override fun observeTransactions(): Flow<List<CoinTransaction>> = rows.map { all -> all.sortedByDescending { it.id } }

    override suspend fun forSession(sessionId: Long): List<CoinTransaction> = rows.value.filter { it.sessionId == sessionId }

    private fun write(amount: Int, kind: CoinKind, sessionId: Long?, note: String?, speciesId: Long? = null) {
        if (amount == 0) return
        rows.value = rows.value + CoinTransaction(
            id = nextId++, at = clock.instant(), amount = amount, kind = kind,
            sessionId = sessionId, speciesId = speciesId, note = note,
        )
    }
}
