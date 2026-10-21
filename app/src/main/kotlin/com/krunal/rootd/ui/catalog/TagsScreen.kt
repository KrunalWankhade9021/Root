package com.krunal.rootd.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krunal.rootd.ui.nav.Route
import com.krunal.rootd.ui.nav.StubScreen

/** Stub from T01 — replaced by T12. */
@Composable
fun TagsScreen(onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    StubScreen(title = "Tags", task = "T12", onNavigate = onNavigate, modifier = modifier)
}
