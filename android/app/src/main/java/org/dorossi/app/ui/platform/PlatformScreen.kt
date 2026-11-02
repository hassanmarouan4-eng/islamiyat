package org.dorossi.app.ui.platform

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dorossi.app.BuildConfig
import org.dorossi.app.R
import org.dorossi.app.appContainer
import org.dorossi.app.core.LinkRules
import org.dorossi.app.core.LinkTarget
import org.dorossi.app.core.WebFiles

/** Colours of the platform's own header, page and bottom bar, so the app frame matches the site. */
private object PlatformColors {
    val barLight = Color(0xFF0A4434)
    val barDark = Color(0xFF0C3428)
    val pageLight = Color(0xFFF2F5F3)
    val pageDark = Color(0xFF0B1512)
    val bottomLight = Color(0xFFFFFFFF)
    val bottomDark = Color(0xFF121F1A)
    val gold = Color(0xFFE2B85A)
    val brand = Color(0xFF0A4434)
}

private const val BACK_SCRIPT =
    "(function(){try{return window.__appBack&&window.__appBack()?1:0}catch(e){return 0}})()"

/**
 * The platform itself, shown in a WebView with what a browser tab cannot do: opening WhatsApp
 * and other apps, real downloads, file uploads, full-screen video and slides, a back button that
 * closes the open file first, protection against screenshots of protected files, and a clear
 * screen when there is no connection.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlatformScreen(
    controller: PlatformController,
    links: StateFlow<String?>,
    onLinkConsumed: () -> Unit,
    onOpenSettings: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val container = appContainer()
    val viewModel: PlatformViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlatformViewModel(container.hostResolver) }
        },
    )
    val target by viewModel.target.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()
    val currentOpenSettings by rememberUpdatedState(onOpenSettings)
    val currentLinkConsumed by rememberUpdatedState(onLinkConsumed)

    fun snack(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    var pendingFiles by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = pendingFiles
        pendingFiles = null
        callback?.onReceiveValue(chosenFiles(result.resultCode, result.data))
    }

    val host = remember(context) {
        object : PlatformHost {
            override fun openExternal(url: String) {
                if (!openLinkOutside(context, url)) snack(context.getString(R.string.no_app_for_link))
            }

            override fun openIntentLink(url: String) {
                if (!openAndroidIntentLink(context, url)) snack(context.getString(R.string.no_app_for_link))
            }

            override fun openFromNewWindow(url: String) {
                when (LinkRules.classify(url)) {
                    LinkTarget.INTERNAL -> controller.load(url)
                    LinkTarget.EXTERNAL -> openExternal(url)
                    LinkTarget.INTENT -> openIntentLink(url)
                    LinkTarget.IGNORE -> Unit
                }
            }

            override fun isOnline(): Boolean = deviceIsOnline(context)

            override fun download(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
                val scheme = LinkRules.scheme(url)
                if (scheme != "https" && scheme != "http") {
                    snack(context.getString(R.string.download_unsupported))
                    return
                }
                val name = enqueueDownload(context, url, userAgent, contentDisposition, mimeType)
                snack(
                    if (name != null) context.getString(R.string.download_started, name)
                    else context.getString(R.string.download_failed),
                )
            }

            override fun showFileChooser(
                callback: ValueCallback<Array<Uri>>,
                params: WebChromeClient.FileChooserParams,
            ): Boolean {
                pendingFiles?.onReceiveValue(null)
                pendingFiles = callback
                val types = WebFiles.pickerMimeTypes(params.acceptTypes)
                val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = if (types.size == 1) types[0] else "*/*"
                    if (types.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray())
                    putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        params.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE,
                    )
                }
                try {
                    fileLauncher.launch(Intent.createChooser(pick, context.getString(R.string.choose_file)))
                } catch (e: ActivityNotFoundException) {
                    pendingFiles = null
                    callback.onReceiveValue(null)
                }
                return true
            }

            override fun onRendererGone() {
                activity?.recreate()
            }

            override fun setSecure(on: Boolean) {
                val window = activity?.window ?: return
                if (on) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            override fun openSettings() {
                currentOpenSettings()
            }
        }
    }

    val pageBackground = if (dark) PlatformColors.pageDark else PlatformColors.pageLight
    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(pageBackground.toArgb())
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.userAgentString = settings.userAgentString + " DorossiApp/" + BuildConfig.VERSION_NAME
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // The app already keeps the page clear of the status and navigation bars.
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ -> WindowInsetsCompat.CONSUMED }
            webViewClient = PlatformWebViewClient(controller, host)
            webChromeClient = PlatformChromeClient(controller, host)
            setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                host.download(url, userAgent, contentDisposition, mimeType)
            }
            val view = this
            addJavascriptInterface(PlatformBridge(host) { view.url }, "DorossiApp")
            controller.webView = this
        }
    }
    DisposableEffect(webView) {
        onDispose {
            if (controller.webView === webView) controller.webView = null
            webView.stopLoading()
            webView.destroy()
        }
    }

    // Opens the chosen address, or a platform link the app was opened with.
    LaunchedEffect(target) {
        val current = target ?: return@LaunchedEffect
        controller.baseUrl = current.base
        val initial = links.value
        if (initial != null && LinkRules.isInternal(initial)) {
            currentLinkConsumed()
            controller.load(LinkRules.rebase(initial, current.base))
        } else {
            controller.load(current.base)
        }
        links.collect { link ->
            if (link != null) {
                currentLinkConsumed()
                if (LinkRules.isInternal(link)) controller.load(LinkRules.rebase(link, current.base))
            }
        }
    }

    val retry: () -> Unit = {
        if (controller.pageShown) controller.reload() else viewModel.resolve()
    }
    val currentRetry by rememberUpdatedState(retry)

    // Without Internet: try again by itself as soon as a connection is back.
    val error = controller.error
    DisposableEffect(error) {
        if (error != LoadError.OFFLINE) return@DisposableEffect onDispose { }
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val main = Handler(Looper.getMainLooper())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                main.post {
                    if (controller.error == LoadError.OFFLINE && controller.allowAutoRetry()) currentRetry()
                }
            }
        }
        val registered = try {
            manager?.registerDefaultNetworkCallback(callback)
            manager != null
        } catch (e: RuntimeException) {
            false
        }
        onDispose {
            if (registered) {
                try {
                    manager?.unregisterNetworkCallback(callback)
                } catch (e: RuntimeException) {
                    // Already unregistered.
                }
            }
        }
    }

    var exitArmed by remember { mutableStateOf(false) }
    val pressAgain = stringResource(R.string.press_back_again)
    BackHandler {
        when {
            controller.customView != null -> controller.hideCustomView()
            !controller.pageShown -> activity?.finish()
            else -> webView.evaluateJavascript(BACK_SCRIPT) { result ->
                if (result == "1") return@evaluateJavascript
                if (webView.canGoBack() && !LinkRules.isHome(webView.url)) {
                    webView.goBack()
                } else if (exitArmed) {
                    activity?.finish()
                } else {
                    exitArmed = true
                    scope.launch { snackbarHostState.showSnackbar(pressAgain) }
                    scope.launch {
                        delay(2_500)
                        exitArmed = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) PlatformColors.bottomDark else PlatformColors.bottomLight),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.safeDrawing)
                    .background(if (dark) PlatformColors.barDark else PlatformColors.barLight),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                    .background(pageBackground),
            ) {
                AndroidView(
                    factory = {
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView
                    },
                    update = { view -> view.setBackgroundColor(pageBackground.toArgb()) },
                    modifier = Modifier.fillMaxSize(),
                )
                if (controller.pageShown && controller.progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { controller.progress / 100f },
                        color = PlatformColors.gold,
                        trackColor = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter),
                    )
                }
                error?.let { kind -> ErrorPanel(kind = kind, onRetry = retry) }
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp),
                )
            }
        }
        if (!controller.pageShown && error == null) BrandLoading()
        controller.customView?.let { view -> FullScreenView(view) }
    }
}

@Composable
private fun ErrorPanel(kind: LoadError, onRetry: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Icon(
                imageVector = if (kind == LoadError.OFFLINE) Icons.Filled.WifiOff else Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    if (kind == LoadError.OFFLINE) R.string.error_offline_title else R.string.error_server_title,
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (kind == LoadError.OFFLINE) R.string.error_offline_message else R.string.error_server_message,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
}

/** Same look as the system splash screen, kept until the first page is painted. */
@Composable
private fun BrandLoading() {
    Surface(color = PlatformColors.brand, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(160.dp),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                color = PlatformColors.gold,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.width(120.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.loading_platform),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

/** A video or slide show the page put in full screen; the system bars are hidden meanwhile. */
@Composable
private fun FullScreenView(view: View) {
    val context = LocalContext.current
    DisposableEffect(view) {
        val window = context.findActivity()?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        bars?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        bars?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { bars?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        key(view) {
            AndroidView(
                factory = {
                    (view.parent as? ViewGroup)?.removeView(view)
                    view
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
