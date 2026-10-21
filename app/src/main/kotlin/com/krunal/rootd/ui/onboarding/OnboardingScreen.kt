package com.krunal.rootd.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.krunal.rootd.ui.nav.Route
import com.krunal.rootd.ui.nav.StubScreen

/** Stub from T01 — replaced by T10. */
@Composable
fun OnboardingScreen(onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    StubScreen(title = "Onboarding", task = "T10", onNavigate = onNavigate, modifier = modifier)
}
