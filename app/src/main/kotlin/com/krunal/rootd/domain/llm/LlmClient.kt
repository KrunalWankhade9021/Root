package com.krunal.rootd.domain.llm

interface LlmClient {
    /** One chat completion asking for a JSON object. Never throws for network/HTTP problems — returns Failure. */
    suspend fun chatJson(model: String, system: String, user: String, timeoutMs: Long): LlmResult
}

sealed interface LlmResult {
    data class Ok(val content: String, val latencyMs: Long, val model: String) : LlmResult

    data class Failure(val kind: Kind, val detail: String) : LlmResult {
        enum class Kind { NO_API_KEY, NETWORK, TIMEOUT, HTTP, BAD_RESPONSE }
    }
}
