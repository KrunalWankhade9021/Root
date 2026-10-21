package com.krunal.rootd.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.krunal.rootd.data.db.entity.CoinTransactionEntity

@Dao
interface CoinDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: CoinTransactionEntity): Long
}
