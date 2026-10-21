package com.krunal.rootd.data

import android.content.Context
import com.krunal.rootd.data.memory.InMemoryAllowListRepository
import com.krunal.rootd.data.memory.InMemoryCatalogRepository
import com.krunal.rootd.data.memory.InMemoryCoinLedger
import com.krunal.rootd.data.memory.InMemoryFocusProfileRepository
import com.krunal.rootd.data.memory.InMemoryPassRequestRepository
import com.krunal.rootd.data.memory.InMemorySessionRepository
import com.krunal.rootd.data.memory.InMemorySettingsRepository
import com.krunal.rootd.domain.repo.AllowListRepository
import com.krunal.rootd.domain.repo.CatalogRepository
import com.krunal.rootd.domain.repo.CoinLedger
import com.krunal.rootd.domain.repo.FocusProfileRepository
import com.krunal.rootd.domain.repo.PassRequestRepository
import com.krunal.rootd.domain.repo.SessionRepository
import com.krunal.rootd.domain.repo.SettingsRepository
import java.time.Clock

/**
 * Data bindings. Owned by opencode (plan §2.4): T04/T05 swap these in-memory bindings for Room/DataStore ones;
 * opencode startup work goes in [onAppStart].
 */
class DataModule(
    @Suppress("unused") private val context: Context,
    private val clock: Clock,
) {
    val sessions: SessionRepository = InMemorySessionRepository()
    val passes: PassRequestRepository = InMemoryPassRequestRepository()
    val catalog: CatalogRepository = InMemoryCatalogRepository(clock)
    val allowLists: AllowListRepository = InMemoryAllowListRepository(sessions, clock)
    val coins: CoinLedger = InMemoryCoinLedger(sessions, passes, catalog, clock)
    val profiles: FocusProfileRepository = InMemoryFocusProfileRepository()
    val settings: SettingsRepository = InMemorySettingsRepository()

    /** Called once per process start, on a background scope, by `RootdApp`. */
    suspend fun onAppStart() {}
}
