package com.krunal.rootd.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krunal.rootd.ui.nav.Route
import com.krunal.rootd.ui.nav.StubScreen

/** Stub from T01 — replaced by T13. */
@Composable
fun SessionDetailScreen(sessionId: Long, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    StubScreen(title = "Session detail #$sessionId", task = "T13", onNavigate = onNavigate, modifier = modifier)
}
