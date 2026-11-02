package org.dorossi.app.ui.platform

import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import java.net.URISyntaxException
import org.dorossi.app.R
import org.dorossi.app.core.LinkRules
import org.dorossi.app.core.WebFiles

/** The activity behind a Compose context (null in previews). */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun deviceIsOnline(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/** Opens [url] in the app that handles it (WhatsApp, browser, phone…). False if none. */
fun openLinkOutside(context: Context, url: String): Boolean {
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/**
 * Opens an Android `intent:` link safely (only apps that accept browser links), falling back to
 * its web address when the app is not installed. False if nothing could open it.
 */
fun openAndroidIntentLink(context: Context, url: String): Boolean {
    val intent = try {
        Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
    } catch (e: URISyntaxException) {
        return false
    }
    intent.addCategory(Intent.CATEGORY_BROWSABLE)
    intent.component = null
    intent.selector = null
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        val fallback = intent.getStringExtra("browser_fallback_url")
        val scheme = fallback?.let { LinkRules.scheme(it) }
        if (fallback != null && (scheme == "https" || scheme == "http")) openLinkOutside(context, fallback) else false
    }
}

/**
 * Hands a platform download (summaries, tests) to Android's download manager, which shows its
 * progress in the notifications and saves it in Downloads. Returns the file name, or null.
 */
fun enqueueDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
): String? {
    val extension = mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    val name = WebFiles.downloadName(contentDisposition, url, extension)
    return try {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            if (!mimeType.isNullOrBlank()) setMimeType(mimeType)
            if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
            CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            setTitle(name)
            setDescription(context.getString(R.string.app_name))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            } else {
                // Android 8–9: the app's own Downloads folder (no storage permission needed).
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, name)
            }
        }
        val manager = context.getSystemService(DownloadManager::class.java) ?: return null
        manager.enqueue(request)
        name
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: IllegalStateException) {
        null
    } catch (e: SecurityException) {
        null
    }
}

/** Files chosen in the system picker (one or several), or null when cancelled. */
fun chosenFiles(resultCode: Int, data: Intent?): Array<Uri>? {
    if (resultCode != Activity.RESULT_OK || data == null) return null
    val clip = data.clipData
    if (clip != null && clip.itemCount > 0) return Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
    return data.data?.let { arrayOf(it) }
}

/** Shares the platform address through WhatsApp, SMS, e-mail… */
fun sharePlatform(context: Context, baseUrl: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, baseUrl))
    }
    try {
        context.startActivity(Intent.createChooser(send, context.getString(R.string.action_share)))
    } catch (e: ActivityNotFoundException) {
        // No app can share text: nothing to do.
    }
}
