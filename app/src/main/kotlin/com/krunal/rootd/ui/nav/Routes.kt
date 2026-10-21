package com.krunal.rootd.ui.nav

/**
 * Every screen in the app (plan T01). Routes with an id expose a `PATTERN` for the NavHost and build their
 * concrete `path` from the id. Screen composables live in their owner's package (plan §2.2).
 */
sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object Start : Route("start")
    data object Focusing : Route("focusing")
    data class SessionResult(val sessionId: Long) : Route("session_result/$sessionId") {
        companion object { const val PATTERN = "session_result/{sessionId}" }
    }
    data object Dashboard : Route("dashboard")
    data class SessionDetail(val sessionId: Long) : Route("session_detail/$sessionId") {
        companion object { const val PATTERN = "session_detail/{sessionId}" }
    }
    data object History : Route("history")
    data object Shop : Route("shop")
    data object Tags : Route("tags")
    data object AllowLists : Route("allow_lists")
    data class AllowListEdit(val id: Long) : Route("allow_list_edit/$id") {
        companion object { const val PATTERN = "allow_list_edit/{id}" }
    }
    data object AlwaysAllowed : Route("always_allowed")
    data object Settings : Route("settings")
    data object WeeklyInsights : Route("weekly_insights")

    companion object {
        const val ARG_SESSION_ID = "sessionId"
        const val ARG_ID = "id"

        /**
         * Bottom navigation tabs, in order. A getter, not a stored val: the companion initializes while `Route`
         * itself is still initializing, before the nested objects exist, so a stored list would contain null.
         */
        val tabs: List<Route> get() = listOf(Start, Dashboard, Shop, Settings)
    }
}
