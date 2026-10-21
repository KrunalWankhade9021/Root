package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.krunal.rootd.data.db.entity.CoinTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: CoinTransactionEntity): Long

    /** Ledger sum; null when the ledger is empty (callers treat null as 0). */
    @Query("SELECT SUM(amount) FROM coin_transactions")
    suspend fun sum(): Int?

    @Query("SELECT SUM(amount) FROM coin_transactions")
    fun observeSum(): Flow<Int?>

    @Query("SELECT * FROM coin_transactions WHERE session_id = :sessionId ORDER BY id")
    suspend fun forSession(sessionId: Long): List<CoinTransactionEntity>

    /** Newest first. */
    @Query("SELECT * FROM coin_transactions ORDER BY id DESC")
    fun observeAll(): Flow<List<CoinTransactionEntity>>
}
