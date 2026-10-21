package com.krunal.rootd.data.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.krunal.rootd.data.db.dao.AllowListAppDao
import com.krunal.rootd.data.db.dao.AllowListDao
import com.krunal.rootd.data.db.dao.CatalogDao
import com.krunal.rootd.data.db.dao.CoinDao
import com.krunal.rootd.data.db.dao.FocusProfileDao
import com.krunal.rootd.data.db.dao.PassRequestDao
import com.krunal.rootd.data.db.dao.SessionDao
import com.krunal.rootd.data.db.entity.AllowListAppEntity
import com.krunal.rootd.data.db.entity.AllowListEntity
import com.krunal.rootd.data.db.entity.AlwaysAllowedAppEntity
import com.krunal.rootd.data.db.entity.CoinTransactionEntity
import com.krunal.rootd.data.db.entity.FocusProfileEntity
import com.krunal.rootd.data.db.entity.PassRequestEntity
import com.krunal.rootd.data.db.entity.SessionEntity
import com.krunal.rootd.data.db.entity.SpeciesEntity
import com.krunal.rootd.data.db.entity.TagEntity
import com.krunal.rootd.domain.model.AlwaysAllowedReason
import java.time.Instant

/**
 * Room database callback to create partial unique index and seed data on first creation.
 */
class RootdDatabaseCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Create partial unique index for single ongoing session
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_single_ongoing " +
            "ON sessions(status) WHERE status = 'ongoing';"
        )
        seedDatabase(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // Re-assert partial unique index in case it was dropped
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_single_ongoing " +
            "ON sessions(status) WHERE status = 'ongoing';"
        )
    }

    private fun seedDatabase(db: SupportSQLiteDatabase) {
        val now = Instant.now().toEpochMilli()

        // Seed tags (data-model §5.1)
        val tagColors = intArrayOf(
            0xFF4A7FD1, // study - blue
            0xFFE09A3E, // work - amber
            0xFF9B6FD1, // read - purple
            0xFF2E7D4F  // exercise - green
        )
        val tagNames = arrayOf("study", "work", "read", "exercise")
        for (i in tagNames.indices) {
            db.execSQL(
                "INSERT OR IGNORE INTO tags (name, color_argb, archived, created_at) VALUES (?, ?, 0, ?)",
                arrayOf(tagNames[i], tagColors[i].toString(), now.toString())
            )
        }

        // Seed species (data-model §5.2) - fixed IDs, not autoincrement
        val speciesData = listOf(
            SpeciesSeed(1, "oak", "Oak", 0, now, 0),
            SpeciesSeed(2, "pine", "Pine", 0, now, 1),
            SpeciesSeed(3, "birch", "Birch", 300, null, 2),
            SpeciesSeed(4, "maple", "Maple", 500, null, 3),
            SpeciesSeed(5, "cherry", "Cherry", 800, null, 4),
            SpeciesSeed(6, "bamboo", "Bamboo", 1000, null, 5),
            SpeciesSeed(7, "baobab", "Baobab", 1500, null, 6)
        )
        for (s in speciesData) {
            val unlockedAt = s.unlockedAt?.toString() ?: "NULL"
            db.execSQL(
                "INSERT OR IGNORE INTO species (id, key, name, price_coins, unlocked_at, sort_order) VALUES (?, ?, ?, ?, $unlockedAt, ?)",
                arrayOf(s.id.toString(), s.key, s.name, s.priceCoins.toString(), s.sortOrder.toString())
            )
        }

        // Seed allow list "Default" (data-model §5.3)
        db.execSQL(
            "INSERT OR IGNORE INTO allow_lists (id, name, is_default, created_at) VALUES (1, 'Default', 1, ?)",
            arrayOf(now.toString())
        )

        // Note: always_allowed_apps are NOT seeded here (T12 resolves them on device)
    }

    private data class SpeciesSeed(
        val id: Long,
        val key: String,
        val name: String,
        val priceCoins: Int,
        val unlockedAt: Long?,
        val sortOrder: Int
    )
}