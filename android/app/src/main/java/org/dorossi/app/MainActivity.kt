package org.dorossi.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.dorossi.app.core.LinkRules
import org.dorossi.app.core.LocaleSupport
import org.dorossi.app.ui.AppRoot
import org.dorossi.app.ui.theme.AppTheme

// AppCompatActivity (not ComponentActivity) so that the in-app language switch works on every
// Android version through AppCompatDelegate.setApplicationLocales.
class MainActivity : AppCompatActivity() {

    /** A platform link (https://dorossi.org/…) the app was opened with, waiting to be shown. */
    private val links = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        LocaleSupport.ensureSupportedLocale()
        // The top of the screen is the platform's dark green header: light status-bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM),
        )
        if (savedInstanceState == null) links.value = platformLink(intent)

        setContent {
            AppTheme(darkTheme = isSystemInDarkTheme()) {
                AppRoot(links = links, onLinkConsumed = { links.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        platformLink(intent)?.let { links.value = it }
    }

    private fun platformLink(intent: Intent?): String? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString?.takeIf { LinkRules.isInternal(it) }

    private companion object {
        val LIGHT_SCRIM: Int = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM: Int = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
