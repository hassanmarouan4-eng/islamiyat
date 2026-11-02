package org.dorossi.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dorossi.app.R
import org.dorossi.app.core.LinkRules
import org.dorossi.app.ui.about.AboutScreen
import org.dorossi.app.ui.platform.PlatformController
import org.dorossi.app.ui.platform.PlatformScreen
import org.dorossi.app.ui.platform.findActivity
import org.dorossi.app.ui.platform.sharePlatform
import org.dorossi.app.ui.settings.SettingsScreen

private enum class Overlay { NONE, SETTINGS, ABOUT }

/**
 * The platform page is always kept alive underneath; settings and about are drawn above it, so
 * closing them returns to the exact page (and open file) the user left.
 */
@Composable
fun AppRoot(links: StateFlow<String?>, onLinkConsumed: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    val controller = remember { PlatformController() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    val cacheCleared = stringResource(R.string.cache_cleared)

    // Light status-bar icons over the platform's green header; dark ones over light settings pages.
    SideEffect {
        val window = context.findActivity()?.window ?: return@SideEffect
        val bars = WindowCompat.getInsetsController(window, view)
        bars.isAppearanceLightStatusBars = overlay != Overlay.NONE && !dark
        bars.isAppearanceLightNavigationBars = !dark
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PlatformScreen(
            controller = controller,
            links = links,
            onLinkConsumed = onLinkConsumed,
            onOpenSettings = { overlay = Overlay.SETTINGS },
            snackbarHostState = snackbarHostState,
        )
        when (overlay) {
            Overlay.SETTINGS -> SettingsScreen(
                onBack = { overlay = Overlay.NONE },
                onReload = {
                    overlay = Overlay.NONE
                    controller.reload()
                },
                onClearCache = {
                    controller.clearCache()
                    overlay = Overlay.NONE
                    controller.reload()
                    scope.launch { snackbarHostState.showSnackbar(cacheCleared) }
                },
                onShare = { sharePlatform(context, controller.baseUrl ?: DEFAULT_SHARE_URL) },
                onOpenAbout = { overlay = Overlay.ABOUT },
            )
            Overlay.ABOUT -> AboutScreen(
                onBack = { overlay = Overlay.SETTINGS },
                platformHost = controller.baseUrl?.let { LinkRules.host(it) },
            )
            Overlay.NONE -> Unit
        }
    }

    // Declared after the platform's own back handling, so it wins while an overlay is open.
    BackHandler(enabled = overlay != Overlay.NONE) {
        overlay = if (overlay == Overlay.ABOUT) Overlay.SETTINGS else Overlay.NONE
    }
}

private const val DEFAULT_SHARE_URL = "https://www.dorossi.org"
