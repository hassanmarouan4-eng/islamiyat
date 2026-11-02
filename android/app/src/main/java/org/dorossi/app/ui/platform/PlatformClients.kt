package org.dorossi.app.ui.platform

import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.dorossi.app.BuildConfig
import org.dorossi.app.core.LinkRules
import org.dorossi.app.core.LinkTarget

/** What the platform page needs from the app (implemented by PlatformScreen). */
interface PlatformHost {
    fun openExternal(url: String)
    fun openIntentLink(url: String)

    /** A link the page opened in a new window (target=_blank, window.open). */
    fun openFromNewWindow(url: String)
    fun isOnline(): Boolean
    fun download(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?)
    fun showFileChooser(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean
    fun onRendererGone()
    fun setSecure(on: Boolean)
    fun openSettings()
}

class PlatformWebViewClient(
    private val controller: PlatformController,
    private val host: PlatformHost,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        if (request == null) return true
        val url = request.url?.toString() ?: return true
        if (!request.isForMainFrame) {
            // Frames inside the page (videos, ads) load web pages only, never other apps.
            val scheme = LinkRules.scheme(url)
            return !(scheme == "http" || scheme == "https" || scheme == "about" || scheme == "blob")
        }
        return when (LinkRules.classify(url)) {
            LinkTarget.INTERNAL -> false
            LinkTarget.EXTERNAL -> {
                host.openExternal(url)
                true
            }
            LinkTarget.INTENT -> {
                host.openIntentLink(url)
                true
            }
            LinkTarget.IGNORE -> true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        controller.onPageStarted()
        // A new page never inherits the screenshot protection of a file viewer left open.
        host.setSecure(false)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        controller.onPageVisible()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        controller.onPageVisible()
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        if (request?.isForMainFrame == true) {
            controller.onMainFrameError(if (host.isOnline()) LoadError.SERVER else LoadError.OFFLINE)
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        // The page engine stopped (low memory): rebuild the screen instead of closing the app.
        host.onRendererGone()
        return true
    }
}

class PlatformChromeClient(
    private val controller: PlatformController,
    private val host: PlatformHost,
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        controller.progress = newProgress
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view == null || callback == null) return
        controller.showCustomView(view, callback)
    }

    override fun onHideCustomView() {
        controller.onCustomViewHidden()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
    ): Boolean {
        if (filePathCallback == null || fileChooserParams == null) return false
        return host.showFileChooser(filePathCallback, fileChooserParams)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        // The platform needs neither camera nor microphone.
        request?.deny()
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, false, false)
    }

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        // Pop-ups without a tap are refused; a tapped link in a new window is caught and routed.
        if (view == null || resultMsg == null || !isUserGesture) return false
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        val popup = WebView(view.context)
        popup.webViewClient = object : WebViewClient() {
            private var handled = false

            private fun handle(target: WebView?, url: String) {
                if (handled) return
                handled = true
                host.openFromNewWindow(url)
                if (target != null) {
                    target.stopLoading()
                    target.post { target.destroy() }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.toString()?.let { handle(view, it) }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null && url != "about:blank") handle(view, url)
            }
        }
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }
}

/**
 * Functions the platform page can call as `window.DorossiApp.…` (see views.js on the site).
 * Calls are honoured only while a platform page is displayed (not from ads or other sites).
 */
class PlatformBridge(
    private val host: PlatformHost,
    private val currentUrl: () -> String?,
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun setSecure(on: Boolean) {
        main.post { if (LinkRules.isInternal(currentUrl())) host.setSecure(on) }
    }

    @JavascriptInterface
    fun openSettings() {
        main.post { if (LinkRules.isInternal(currentUrl())) host.openSettings() }
    }

    @JavascriptInterface
    fun appVersion(): String = BuildConfig.VERSION_NAME
}
