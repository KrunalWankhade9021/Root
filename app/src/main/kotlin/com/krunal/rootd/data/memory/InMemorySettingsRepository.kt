package com.krunal.rootd.data.memory

import com.krunal.rootd.domain.model.Settings
import com.krunal.rootd.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Non-persistent settings for tests and pre-T04 builds. The API key is held in memory only. */
class InMemorySettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    private var key = ""

    override val settings: Flow<Settings> = state

    override suspend fun current(): Settings = state.value

    override suspend fun update(transform: (Settings) -> Settings) = state.update(transform)

    override suspend fun apiKey(): String = key

    override suspend fun setApiKey(key: String) {
        this.key = key
    }
}
