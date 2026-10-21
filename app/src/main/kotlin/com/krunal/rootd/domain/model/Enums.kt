package com.krunal.rootd.domain.model

// Stored enum values — exact strings from docs/data-model.md §2. `db` is what goes into SQLite;
// `fromDb` rejects anything else so corrupted rows surface instead of silently mapping.

private inline fun <reified E : Enum<E>> parse(value: String, db: (E) -> String): E =
    enumValues<E>().firstOrNull { db(it) == value }
        ?: throw IllegalArgumentException("Unknown ${E::class.simpleName} value: '$value'")

enum class SessionMode(val db: String) {
    TIMER("timer"), STOPWATCH("stopwatch");
    companion object { fun fromDb(v: String): SessionMode = parse(v) { it.db } }
}

enum class SessionStatus(val db: String) {
    ONGOING("ongoing"), GROWN("grown"), WITHERED("withered"), CANCELLED("cancelled");
    companion object { fun fromDb(v: String): SessionStatus = parse(v) { it.db } }
}

enum class FailReason(val db: String) {
    GAVE_UP("gave_up"), STOPWATCH_TOO_SHORT("stopwatch_too_short");
    companion object { fun fromDb(v: String): FailReason = parse(v) { it.db } }
}

enum class Verdict(val db: String) {
    ALLOW("allow"), PUSH_BACK("push_back"), DENY("deny");
    companion object { fun fromDb(v: String): Verdict = parse(v) { it.db } }
}

enum class VerdictSource(val db: String) {
    LLM("llm"), FALLBACK("fallback");
    companion object { fun fromDb(v: String): VerdictSource = parse(v) { it.db } }
}

enum class ReasonCategory(val db: String) {
    WORK_MESSAGE("work_message"), PERSONAL_MESSAGE("personal_message"),
    CALL_OR_MEETING("call_or_meeting"), LOOKUP_INFO("lookup_info"), PAYMENT_OR_OTP("payment_or_otp"),
    ENTERTAINMENT("entertainment"), JUST_CHECKING("just_checking"), OTHER("other");
    companion object { fun fromDb(v: String): ReasonCategory = parse(v) { it.db } }
}

enum class PassEndReason(val db: String) {
    EXPIRED("expired"), RETURNED_TO_ROOTD("returned_to_rootd"), SESSION_ENDED("session_ended");
    companion object { fun fromDb(v: String): PassEndReason = parse(v) { it.db } }
}

enum class CoinKind(val db: String) {
    SESSION_REWARD("session_reward"), NO_PASS_BONUS("no_pass_bonus"),
    PASS_COST("pass_cost"), SPECIES_UNLOCK("species_unlock");
    companion object { fun fromDb(v: String): CoinKind = parse(v) { it.db } }
}

enum class AlwaysAllowedReason(val db: String) {
    PHONE("phone"), MAPS("maps"), OTP("otp"), USER("user");
    companion object { fun fromDb(v: String): AlwaysAllowedReason = parse(v) { it.db } }
}
