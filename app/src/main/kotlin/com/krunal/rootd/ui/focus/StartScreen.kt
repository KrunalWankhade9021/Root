package com.krunal.rootd.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krunal.rootd.ui.nav.Route
import com.krunal.rootd.ui.nav.StubScreen

/** Stub from T01 — replaced by T08. */
@Composable
fun StartScreen(onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    StubScreen(title = "Start", task = "T08", onNavigate = onNavigate, modifier = modifier)
}
