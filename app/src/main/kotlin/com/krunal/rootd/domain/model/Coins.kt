package com.krunal.rootd.domain.model

import java.time.Instant

/** One row of the coin ledger (data-model §6). Sign rules: rewards/bonus > 0, costs/unlocks < 0. */
data class CoinTransaction(
    val id: Long = 0,
    val at: Instant,
    val amount: Int,
    val kind: CoinKind,
    val sessionId: Long?,
    val speciesId: Long?,
    val note: String?,
)
