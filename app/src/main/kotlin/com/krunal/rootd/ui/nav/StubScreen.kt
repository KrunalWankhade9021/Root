package com.krunal.rootd.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Placeholder used by every T01 stub screen. It shows which task replaces the screen and links to every route so
 * the whole nav graph can be walked on a device (T01 AC-5). Owners delete their usage when they build the screen.
 */
@Composable
fun StubScreen(title: String, task: String, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("$title — TODO $task", style = MaterialTheme.typography.headlineSmall)
        allRoutesForStubs().forEach { route ->
            OutlinedButton(onClick = { onNavigate(route) }) { Text(route.path) }
        }
    }
}

private fun allRoutesForStubs(): List<Route> = listOf(
    Route.Onboarding, Route.Start, Route.Focusing, Route.SessionResult(1), Route.Dashboard, Route.SessionDetail(1),
    Route.History, Route.Shop, Route.Tags, Route.AllowLists, Route.AllowListEdit(1), Route.AlwaysAllowed,
    Route.Settings, Route.WeeklyInsights,
)
