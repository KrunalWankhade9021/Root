package com.krunal.rootd.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
import java.time.Clock

/** Schema version 1 = docs/data-model.md. Never use fallbackToDestructiveMigration (data-model §10). */
@Database(
    entities = [
        SessionEntity::class, PassRequestEntity::class, TagEntity::class, SpeciesEntity::class,
        AllowListEntity::class, AllowListAppEntity::class, AlwaysAllowedAppEntity::class,
        CoinTransactionEntity::class, FocusProfileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RootdDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun passRequestDao(): PassRequestDao
    abstract fun catalogDao(): CatalogDao
    abstract fun allowListDao(): AllowListDao
    abstract fun coinDao(): CoinDao
    abstract fun focusProfileDao(): FocusProfileDao

    companion object {
        const val FILE_NAME = "rootd.db"

        /** Exact SQL from data-model §8. Room can't declare partial indexes, so the callback creates it. */
        const val SINGLE_ONGOING_INDEX_SQL =
            "CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_single_ongoing ON sessions(status) WHERE status = 'ongoing'"

        fun create(context: Context, clock: Clock): RootdDatabase =
            Room.databaseBuilder(context.applicationContext, RootdDatabase::class.java, FILE_NAME)
                .addCallback(callback(clock))
                .build()

        /** For tests only. */
        fun inMemory(context: Context, clock: Clock): RootdDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, RootdDatabase::class.java)
                .addCallback(callback(clock))
                .allowMainThreadQueries()
                .build()

        private fun callback(clock: Clock) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(SINGLE_ONGOING_INDEX_SQL)
                Seed.run(db, clock.instant())
            }

            /** Re-asserted on every open (idempotent) so the invariant index can never be missing. */
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL(SINGLE_ONGOING_INDEX_SQL)
            }
        }
    }
}
