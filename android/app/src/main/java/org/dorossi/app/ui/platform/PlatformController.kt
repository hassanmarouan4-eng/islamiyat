package org.dorossi.app.ui.platform

import android.os.SystemClock
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Why the platform page could not be shown. */
enum class LoadError { OFFLINE, SERVER }

/**
 * State of the platform page, shared by the WebView callbacks and the Compose UI.
 * Every member is used on the main thread only.
 */
class PlatformController {
    var webView: WebView? = null

    /** Address in use (https://www.dorossi.org, or a fallback), once chosen. */
    var baseUrl by mutableStateOf<String?>(null)

    var progress by mutableIntStateOf(0)

    /** True once a platform page has been displayed successfully. */
    var pageShown by mutableStateOf(false)
        private set

    var error by mutableStateOf<LoadError?>(null)
        private set

    /** Full-screen element (video, slide show) shown above everything, or null. */
    var customView by mutableStateOf<View?>(null)
        private set

    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var mainFrameFailed = false
    private var lastAutoRetry = 0L

    fun load(url: String) {
        error = null
        webView?.loadUrl(url)
    }

    fun reload() {
        error = null
        webView?.reload()
    }

    fun clearCache() {
        webView?.clearCache(true)
    }

    fun onPageStarted() {
        mainFrameFailed = false
    }

    fun onPageVisible() {
        if (mainFrameFailed) return
        pageShown = true
        error = null
    }

    fun onMainFrameError(kind: LoadError) {
        mainFrameFailed = true
        error = kind
    }

    /** Limits automatic retries (when the connection comes back) to one every few seconds. */
    fun allowAutoRetry(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAutoRetry < AUTO_RETRY_INTERVAL_MS) return false
        lastAutoRetry = now
        return true
    }

    fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (customView != null) hideCustomView()
        customView = view
        customViewCallback = callback
    }

    /** Called by the page when it leaves full screen by itself. */
    fun onCustomViewHidden() {
        customView = null
        customViewCallback = null
    }

    /** Leaves full screen from the app side (back button). */
    fun hideCustomView() {
        val callback = customViewCallback
        customView = null
        customViewCallback = null
        callback?.onCustomViewHidden()
    }

    private companion object {
        const val AUTO_RETRY_INTERVAL_MS = 5_000L
    }
}
