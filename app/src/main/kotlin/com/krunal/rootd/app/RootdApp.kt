package com.krunal.rootd.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RootdApp : Application() {

    lateinit var container: AppContainer
        private set

    /** Process-wide scope for startup and background work that must outlive any screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, DeviceClock)
        appScope.launch { container.data.onAppStart() }
    }
}
