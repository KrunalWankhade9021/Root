package com.krunal.rootd.ui.nav

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.krunal.rootd.ui.theme.RootdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** T01 AC-5: the shell starts on Start, the four tabs work, and every route can be opened. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RootdNavHostTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContent() = compose.setContent { RootdTheme { RootdNavHost() } }

    @Test
    fun startsOnStartScreen() {
        setContent()
        compose.onNodeWithText("Start — TODO T08").assertIsDisplayed()
    }

    @Test
    fun bottomTabs_openDashboardShopSettings() {
        setContent()
        compose.onNodeWithText("Dashboard").performClick()
        compose.onNodeWithText("Dashboard — TODO T11").assertIsDisplayed()
        compose.onNodeWithText("Shop").performClick()
        compose.onNodeWithText("Shop — TODO T13").assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Settings — TODO T10").assertIsDisplayed()
        compose.onNodeWithText("Focus").performClick()
        compose.onNodeWithText("Start — TODO T08").assertIsDisplayed()
    }

    @Test
    fun everyRoute_isNavigable() {
        val expected = listOf(
            "onboarding" to "Onboarding — TODO T10",
            "focusing" to "Focusing — TODO T08",
            "session_result/1" to "Session result #1 — TODO T08",
            "session_detail/1" to "Session detail #1 — TODO T13",
            "history" to "History — TODO T13",
            "tags" to "Tags — TODO T12",
            "allow_lists" to "Allow Lists — TODO T12",
            "allow_list_edit/1" to "Edit Allow List #1 — TODO T12",
            "always_allowed" to "Always Allowed — TODO T12",
            "weekly_insights" to "Weekly insights — TODO T15",
        )
        setContent()
        expected.forEach { (path, title) ->
            compose.onAllNodesWithText(path).onFirst().performScrollTo().performClick()
            compose.onNodeWithText(title).assertIsDisplayed()
        }
    }
}
