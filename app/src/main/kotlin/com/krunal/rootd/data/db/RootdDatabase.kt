package com.krunal.rootd.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

@Database(
    entities = [
        SessionEntity::class,
        PassRequestEntity::class,
        TagEntity::class,
        SpeciesEntity::class,
        AllowListEntity::class,
        AllowListAppEntity::class,
        AlwaysAllowedAppEntity::class,
        CoinTransactionEntity::class,
        FocusProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RootdDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun passRequestDao(): PassRequestDao
    abstract fun catalogDao(): CatalogDao
    abstract fun allowListDao(): AllowListDao
    abstract fun allowListAppDao(): AllowListAppDao
    abstract fun coinDao(): CoinDao
    abstract fun focusProfileDao(): FocusProfileDao

    companion object {
        private const val DB_NAME = "rootd.db"

        @Volatile
        private var INSTANCE: RootdDatabase? = null

        fun create(context: Context): RootdDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RootdDatabase::class.java,
                    DB_NAME
                )
                    .addCallback(RootdDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun inMemory(context: Context): RootdDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                RootdDatabase::class.java
            )
                .addCallback(RootdDatabaseCallback())
                .allowMainThreadQueries()
                .build()
        }
    }
}