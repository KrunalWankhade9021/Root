package com.krunal.rootd.app

import android.content.Context
import com.krunal.rootd.data.DataModule
import java.time.Clock

/** opencode-owned UI wiring (plan §2.4): dashboard, catalog, shop/history, widgets. */
class UiModule(
    @Suppress("unused") private val context: Context,
    @Suppress("unused") private val clock: Clock,
    @Suppress("unused") private val data: DataModule,
)
