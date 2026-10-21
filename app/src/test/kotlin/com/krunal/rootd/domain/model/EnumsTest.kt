package com.krunal.rootd.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EnumsTest {

    @Test
    fun fromDb_roundTripsEveryValue() {
        SessionMode.entries.forEach { assertEquals(it, SessionMode.fromDb(it.db)) }
        SessionStatus.entries.forEach { assertEquals(it, SessionStatus.fromDb(it.db)) }
        FailReason.entries.forEach { assertEquals(it, FailReason.fromDb(it.db)) }
        Verdict.entries.forEach { assertEquals(it, Verdict.fromDb(it.db)) }
        VerdictSource.entries.forEach { assertEquals(it, VerdictSource.fromDb(it.db)) }
        ReasonCategory.entries.forEach { assertEquals(it, ReasonCategory.fromDb(it.db)) }
        PassEndReason.entries.forEach { assertEquals(it, PassEndReason.fromDb(it.db)) }
        CoinKind.entries.forEach { assertEquals(it, CoinKind.fromDb(it.db)) }
        AlwaysAllowedReason.entries.forEach { assertEquals(it, AlwaysAllowedReason.fromDb(it.db)) }
    }

    @Test
    fun dbValues_matchDataModelSection2() {
        assertEquals(listOf("timer", "stopwatch"), SessionMode.entries.map { it.db })
        assertEquals(listOf("ongoing", "grown", "withered", "cancelled"), SessionStatus.entries.map { it.db })
        assertEquals(listOf("gave_up", "stopwatch_too_short"), FailReason.entries.map { it.db })
        assertEquals(listOf("allow", "push_back", "deny"), Verdict.entries.map { it.db })
        assertEquals(listOf("llm", "fallback"), VerdictSource.entries.map { it.db })
        assertEquals(
            listOf(
                "work_message", "personal_message", "call_or_meeting", "lookup_info",
                "payment_or_otp", "entertainment", "just_checking", "other",
            ),
            ReasonCategory.entries.map { it.db },
        )
        assertEquals(listOf("expired", "returned_to_rootd", "session_ended"), PassEndReason.entries.map { it.db })
        assertEquals(
            listOf("session_reward", "no_pass_bonus", "pass_cost", "species_unlock"),
            CoinKind.entries.map { it.db },
        )
        assertEquals(listOf("phone", "maps", "otp", "user"), AlwaysAllowedReason.entries.map { it.db })
    }

    @Test
    fun fromDb_unknownValue_throws() {
        assertThrows(IllegalArgumentException::class.java) { SessionMode.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { SessionStatus.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { FailReason.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { Verdict.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { VerdictSource.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { ReasonCategory.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { PassEndReason.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { CoinKind.fromDb("nope") }
        assertThrows(IllegalArgumentException::class.java) { AlwaysAllowedReason.fromDb("nope") }
    }
}
