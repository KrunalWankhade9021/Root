package com.krunal.rootd.app

import android.content.Context
import com.krunal.rootd.data.DataModule
import java.time.Clock

/** Manual DI root (plan §2.4). Each agent edits only its own module file. */
class AppContainer(context: Context, val clock: Clock) {
    val data: DataModule = DataModule(context, clock)
    val features: FeatureModule = FeatureModule(context, clock, data)
    val ui: UiModule = UiModule(context, clock, data)
}
