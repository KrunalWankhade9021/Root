package com.krunal.rootd.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant

/** First-create seed data, data-model §5. Always-allowed apps are resolved on device by T12, not seeded here. */
internal object Seed {

    private val tags = listOf(
        "study" to 0xFF4A7FD1.toInt(),
        "work" to 0xFFE09A3E.toInt(),
        "read" to 0xFF9B6FD1.toInt(),
        "exercise" to 0xFF2E7D4F.toInt(),
    )

    /** (id, key, name, price). Starters (price 0) are unlocked at install. Prices are tunable (data-model §5.2). */
    private val species = listOf(
        Species(1, "oak", "Oak", 0),
        Species(2, "pine", "Pine", 0),
        Species(3, "birch", "Birch", 300),
        Species(4, "maple", "Maple", 500),
        Species(5, "cherry", "Cherry", 800),
        Species(6, "bamboo", "Bamboo", 1000),
        Species(7, "baobab", "Baobab", 1500),
    )

    private data class Species(val id: Long, val key: String, val name: String, val price: Int)

    fun run(db: SupportSQLiteDatabase, now: Instant) {
        val ms = now.toEpochMilli()
        tags.forEach { (name, color) ->
            db.execSQL("INSERT INTO tags (name, color_argb, archived, created_at) VALUES (?, ?, 0, ?)", arrayOf<Any?>(name, color, ms))
        }
        species.forEachIndexed { order, s ->
            db.execSQL(
                "INSERT INTO species (id, key, name, price_coins, unlocked_at, sort_order) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(s.id, s.key, s.name, s.price, if (s.price == 0) ms else null, order),
            )
        }
        db.execSQL("INSERT INTO allow_lists (name, is_default, created_at) VALUES ('Default', 1, ?)", arrayOf<Any?>(ms))
    }
}
