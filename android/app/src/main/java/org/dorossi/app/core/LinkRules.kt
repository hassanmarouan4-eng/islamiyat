package org.dorossi.app.core

/** Addresses of the platform. The first one is preferred; the others are fallbacks. */
object PlatformHosts {
    val candidates: List<String> = listOf(
        "https://dorossi.org",
        "https://islamiyat.hassanmarouan4.workers.dev",
    )

    /** Hosts whose pages open inside the app (everything else opens in another app). */
    val internal: Set<String> = setOf(
        "dorossi.org",
        "www.dorossi.org",
        "islamiyat.hassanmarouan4.workers.dev",
    )
}

/** What the app does with a link the page wants to open. */
enum class LinkTarget {
    /** A platform page: stays in the app. */
    INTERNAL,

    /** Another website or app (WhatsApp, Facebook, YouTube, phone, e-mail…): opened outside. */
    EXTERNAL,

    /** An Android `intent:` link, opened with its own rules. */
    INTENT,

    /** Never opened (local files, scripts, data URLs). */
    IGNORE,
}

/** Pure link rules (no Android classes), unit-tested in LinkRulesTest. */
object LinkRules {

    private val openableSchemes = setOf("about", "blob")
    private val blockedSchemes = setOf("javascript", "data", "file", "content")

    fun scheme(url: String): String? {
        val colon = url.indexOf(':')
        if (colon <= 0) return null
        val s = url.substring(0, colon)
        return if (s.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) s.lowercase() else null
    }

    /** Lower-case host of an http(s) URL, without user info or port. */
    fun host(url: String): String? {
        val origin = splitOrigin(url)?.first ?: return null
        val authority = origin.substringAfter("://").substringAfterLast('@')
        val host = if (authority.startsWith("[")) authority.substringBefore(']') + "]" else authority.substringBefore(':')
        return host.lowercase().trimEnd('.').ifEmpty { null }
    }

    private fun isInternalHost(host: String?): Boolean = host != null && host in PlatformHosts.internal

    fun isInternal(url: String?): Boolean {
        if (url == null) return false
        val s = scheme(url)
        return (s == "https" || s == "http") && isInternalHost(host(url))
    }

    fun classify(url: String): LinkTarget {
        val s = scheme(url) ?: return LinkTarget.IGNORE
        return when {
            s == "http" || s == "https" -> if (isInternalHost(host(url))) LinkTarget.INTERNAL else LinkTarget.EXTERNAL
            s == "intent" -> LinkTarget.INTENT
            s in openableSchemes -> LinkTarget.INTERNAL
            s in blockedSchemes -> LinkTarget.IGNORE
            else -> LinkTarget.EXTERNAL
        }
    }

    /**
     * Moves a platform link onto [base] (same path, query and #route) so that it opens with the
     * account already activated on that address. Other links are returned unchanged.
     */
    fun rebase(url: String, base: String): String {
        if (!isInternal(url)) return url
        val rest = splitOrigin(url)?.second ?: return url
        return base.trimEnd('/') + rest
    }

    /** True for the platform's home page ("/", "/#", "/#home"), where "back" leaves the app. */
    fun isHome(url: String?): Boolean {
        if (url == null || !isInternal(url)) return false
        val rest = splitOrigin(url)?.second ?: return true
        val path = rest.substringBefore('#').substringBefore('?')
        val fragment = if (rest.contains('#')) rest.substringAfter('#') else ""
        return (path.isEmpty() || path == "/") && (fragment.isEmpty() || fragment == "home")
    }

    /** "https://host:port" and the rest ("/path?query#fragment"), or null when there is no "://". */
    private fun splitOrigin(url: String): Pair<String, String>? {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd <= 0) return null
        val hostStart = schemeEnd + 3
        var restStart = url.indexOfAny(charArrayOf('/', '?', '#'), hostStart)
        if (restStart < 0) restStart = url.length
        return url.substring(0, restStart) to url.substring(restStart)
    }
}
