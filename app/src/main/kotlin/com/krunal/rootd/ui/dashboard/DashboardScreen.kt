package com.krunal.rootd.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krunal.rootd.ui.nav.Route
import com.krunal.rootd.ui.nav.StubScreen

/** Stub from T01 — replaced by T11. */
@Composable
fun DashboardScreen(onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    StubScreen(title = "Dashboard", task = "T11", onNavigate = onNavigate, modifier = modifier)
}
