package com.krunal.rootd.domain.repo

import com.krunal.rootd.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun current(): Settings
    suspend fun update(transform: (Settings) -> Settings)

    /** The LLM API key, or "" if unset. Never log or export it. */
    suspend fun apiKey(): String
    suspend fun setApiKey(key: String)
}
