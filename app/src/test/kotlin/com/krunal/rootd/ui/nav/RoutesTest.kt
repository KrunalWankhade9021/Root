package com.krunal.rootd.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun idRoutes_buildPathsThatMatchTheirPattern() {
        assertEquals("session_result/42", Route.SessionResult(42).path)
        assertEquals("session_result/{sessionId}", Route.SessionResult.PATTERN)
        assertEquals("session_detail/7", Route.SessionDetail(7).path)
        assertEquals("allow_list_edit/3", Route.AllowListEdit(3).path)
        listOf(
            Route.SessionResult.PATTERN to Route.SessionResult(1).path,
            Route.SessionDetail.PATTERN to Route.SessionDetail(1).path,
            Route.AllowListEdit.PATTERN to Route.AllowListEdit(1).path,
        ).forEach { (pattern, path) ->
            assertEquals(pattern.substringBefore('{'), path.substringBeforeLast('/') + "/")
        }
    }

    @Test
    fun bottomTabs_areStartDashboardShopSettings() {
        assertEquals(listOf(Route.Start, Route.Dashboard, Route.Shop, Route.Settings), Route.tabs)
    }

    @Test
    fun allPaths_areUnique() {
        val paths = listOf(
            Route.Onboarding, Route.Start, Route.Focusing, Route.Dashboard, Route.History, Route.Shop, Route.Tags,
            Route.AllowLists, Route.AlwaysAllowed, Route.Settings, Route.WeeklyInsights,
        ).map { it.path } + listOf(Route.SessionResult.PATTERN, Route.SessionDetail.PATTERN, Route.AllowListEdit.PATTERN)
        assertEquals(paths.size, paths.toSet().size)
    }
}
