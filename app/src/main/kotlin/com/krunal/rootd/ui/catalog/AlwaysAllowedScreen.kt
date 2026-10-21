package com.krunal.rootd.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krunal.rootd.ui.nav.Route
import com.krunal.rootd.ui.nav.StubScreen

/** Stub from T01 — replaced by T12. */
@Composable
fun AlwaysAllowedScreen(onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    StubScreen(title = "Always Allowed", task = "T12", onNavigate = onNavigate, modifier = modifier)
}
