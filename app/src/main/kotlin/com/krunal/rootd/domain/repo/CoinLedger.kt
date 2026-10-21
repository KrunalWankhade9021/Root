package com.krunal.rootd.domain.repo

import com.krunal.rootd.domain.model.CoinTransaction
import kotlinx.coroutines.flow.Flow

interface CoinLedger {
    fun observeBalance(): Flow<Int>
    suspend fun balance(): Int

    /** Writes the coin rows for a finished session (data-model §6.3). Idempotent; no-op for cancelled/ongoing. */
    suspend fun settleSession(sessionId: Long)
    suspend fun unlockSpecies(speciesId: Long): UnlockResult

    /** Newest first. */
    fun observeTransactions(): Flow<List<CoinTransaction>>
    suspend fun forSession(sessionId: Long): List<CoinTransaction>
}

enum class UnlockResult { UNLOCKED, ALREADY_UNLOCKED, NOT_ENOUGH_COINS }
