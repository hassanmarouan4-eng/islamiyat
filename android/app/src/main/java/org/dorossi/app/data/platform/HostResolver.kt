package org.dorossi.app.data.platform

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.dorossi.app.core.PlatformHosts
import org.dorossi.app.data.settings.SettingsRepository

/**
 * Chooses the platform address: the one used last time if it still answers, otherwise the first
 * candidate that serves the platform (dorossi.org, then the Cloudflare address).
 */
class HostResolver(private val settings: SettingsRepository) {

    suspend fun resolve(): String = withContext(Dispatchers.IO) {
        val saved = settings.baseUrl.first()?.takeIf { it in PlatformHosts.candidates }
        val order = (listOfNotNull(saved) + PlatformHosts.candidates).distinct()
        for (base in order) {
            if (servesPlatform(base)) {
                if (base != saved) settings.setBaseUrl(base)
                return@withContext base
            }
        }
        // Nothing answers (usually no Internet): load the usual address so the page shows the error.
        saved ?: PlatformHosts.candidates.first()
    }

    /** True when [base] answers with the platform's web manifest (small JSON file). */
    private fun servesPlatform(base: String): Boolean {
        return try {
            val connection = URL("$base/manifest.webmanifest").openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            try {
                connection.responseCode == HttpURLConnection.HTTP_OK &&
                    (connection.contentType ?: "").contains("manifest+json")
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private companion object {
        const val TIMEOUT_MS = 6_000
    }
}
