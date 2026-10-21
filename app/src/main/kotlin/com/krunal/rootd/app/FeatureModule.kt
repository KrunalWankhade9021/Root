package com.krunal.rootd.app

import android.content.Context
import com.krunal.rootd.data.DataModule
import java.time.Clock

/** Claude-owned feature wiring (plan §2.4): session engine, gatekeeper, LLM client, blocking, focus/pass UI. */
class FeatureModule(
    @Suppress("unused") private val context: Context,
    @Suppress("unused") private val clock: Clock,
    @Suppress("unused") private val data: DataModule,
)
