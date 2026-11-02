package org.dorossi.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
        // After a crash, show what happened first (plain views, nothing that could crash again).
        val crash = if (savedInstanceState == null) CrashLog.take(this) else null
        if (crash != null) {
            showCrashReport(crash)
            return
        }
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

    private fun showCrashReport(report: String) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val title = TextView(this).apply {
            text = getString(R.string.crash_title)
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(pad, pad, pad, pad / 2)
        }
        val hint = TextView(this).apply {
            text = getString(R.string.crash_hint)
            textSize = 15f
            setPadding(pad, 0, pad, pad / 2)
        }
        val send = Button(this).apply {
            text = getString(R.string.crash_send)
            setOnClickListener {
                val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report)
                try {
                    startActivity(Intent.createChooser(share, getString(R.string.crash_send)))
                } catch (e: ActivityNotFoundException) {
                    // Nothing can share text: the report stays on screen for a screenshot.
                }
            }
        }
        val again = Button(this).apply {
            text = getString(R.string.crash_continue)
            setOnClickListener { recreate() }
        }
        val details = TextView(this).apply {
            text = report
            textSize = 11f
            typeface = Typeface.MONOSPACE
            textDirection = View.TEXT_DIRECTION_LTR
            setTextIsSelectable(true)
            setPadding(pad, pad / 2, pad, pad)
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(hint)
            addView(send)
            addView(again)
            addView(details)
        }
        setContentView(
            ScrollView(this).apply {
                fitsSystemWindows = true
                addView(column)
            },
        )
    }

    private fun platformLink(intent: Intent?): String? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString?.takeIf { LinkRules.isInternal(it) }

    private companion object {
        val LIGHT_SCRIM: Int = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM: Int = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
