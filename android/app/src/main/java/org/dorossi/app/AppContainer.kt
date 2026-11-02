package org.dorossi.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.dorossi.app.data.platform.HostResolver
import org.dorossi.app.data.settings.SettingsRepository

/** Creates and holds the app's long-lived objects. */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val hostResolver: HostResolver by lazy { HostResolver(settingsRepository) }
}

/** The [AppContainer] for use inside composables. */
@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as App).container
