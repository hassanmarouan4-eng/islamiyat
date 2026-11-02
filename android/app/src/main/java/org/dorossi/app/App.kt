package org.dorossi.app

import android.app.Application
import android.content.Context

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Before every library initialiser, so even a crash at start-up is recorded.
        CrashLog.install(base)
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
