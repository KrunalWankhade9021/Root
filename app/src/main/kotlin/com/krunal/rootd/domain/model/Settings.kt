package com.krunal.rootd.domain.model

/** User settings, data-model §9 minus the API key (which lives only in encrypted storage). */
data class Settings(
    val llmBaseUrl: String = "https://api.groq.com/openai/v1",
    val gatekeeperModel: String = "",
    val profileModel: String = "",
    val llmTimeoutMs: Int = 4000,
    val defaultMode: SessionMode = SessionMode.TIMER,
    val defaultMinutes: Int = 25,
    val defaultSpeciesId: Long? = null,
    val defaultAllowListId: Long? = null,
    val dndEnabled: Boolean = false,
    val dndPreviousFilter: Int = -1,
    val ambientSound: String = "none",
    val onboardingDone: Boolean = false,
)
