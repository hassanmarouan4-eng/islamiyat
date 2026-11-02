package org.dorossi.app.core

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/** File names for downloads and file types for upload pickers (pure, unit-tested in WebFilesTest). */
object WebFiles {

    private const val MAX_NAME_LENGTH = 120
    private val forbidden = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private val starName = Regex("""filename\*\s*=\s*([^']*)'[^']*'([^;]+)""", RegexOption.IGNORE_CASE)
    private val quotedName = Regex("""filename\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
    private val plainName = Regex("""filename\s*=\s*([^;]+)""", RegexOption.IGNORE_CASE)

    /**
     * Name to save a download under: the server's Content-Disposition name (RFC 5987 UTF-8 names
     * included, as the platform sends Arabic titles), else the last part of the URL, else "file";
     * the extension of [extensionFromMime] is added when the name has none.
     */
    fun downloadName(contentDisposition: String?, url: String, extensionFromMime: String?): String {
        val raw = dispositionName(contentDisposition) ?: lastSegment(url) ?: "file"
        var name = sanitize(raw).ifEmpty { "file" }
        val ext = extensionFromMime?.trim()?.trimStart('.')
        if (!name.contains('.') && !ext.isNullOrEmpty()) name = "$name.$ext"
        return name
    }

    fun dispositionName(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null
        starName.find(contentDisposition)?.let { match ->
            val charsetName = match.groupValues[1].trim().ifEmpty { "UTF-8" }
            val decoded = percentDecode(match.groupValues[2].trim().trim('"'), charsetName)
            if (!decoded.isNullOrBlank()) return decoded
        }
        val plain = quotedName.find(contentDisposition) ?: plainName.find(contentDisposition)
        return plain?.groupValues?.get(1)?.trim()?.trim('"')?.takeIf { it.isNotEmpty() }
    }

    fun lastSegment(url: String): String? {
        val path = url.substringBefore('#').substringBefore('?').trimEnd('/')
        if (!path.contains("://")) return null
        val afterHost = path.substringAfter("://")
        if (!afterHost.contains('/')) return null
        val segment = afterHost.substringAfterLast('/')
        return (percentDecode(segment, "UTF-8") ?: segment).takeIf { it.isNotBlank() }
    }

    /** Removes characters Android file systems refuse and keeps names reasonably short. */
    fun sanitize(name: String): String {
        val clean = name.replace(forbidden, "_").trim().trim('.').trim()
        if (clean.length <= MAX_NAME_LENGTH) return clean
        val ext = clean.substringAfterLast('.', "")
        return if (ext.isNotEmpty() && ext.length < 10) {
            clean.substringBeforeLast('.').take(MAX_NAME_LENGTH - ext.length - 1) + "." + ext
        } else {
            clean.take(MAX_NAME_LENGTH)
        }
    }

    /** Decodes %XX sequences as bytes of [charsetName]; "+" stays a plus sign. Null if malformed. */
    fun percentDecode(value: String, charsetName: String): String? {
        return try {
            val bytes = ByteArrayOutputStream()
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '%') {
                    if (i + 2 >= value.length) return null
                    val hex = value.substring(i + 1, i + 3).toIntOrNull(16) ?: return null
                    bytes.write(hex)
                    i += 3
                } else {
                    val encoded = c.toString().toByteArray(Charsets.UTF_8)
                    bytes.write(encoded, 0, encoded.size)
                    i++
                }
            }
            String(bytes.toByteArray(), Charset.forName(charsetName))
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * MIME types for the system file picker from an `<input accept="…">` list. Only plain MIME
     * types (such as "image/png" or all images) are used as a filter; when the page asks for extensions
     * (`.pptx,.docx`), every file is offered, because phones often report Office files and fonts
     * with a generic type and would hide them. The page itself checks the chosen file.
     */
    fun pickerMimeTypes(accept: Array<String>?): List<String> {
        val entries = accept.orEmpty()
            .flatMap { it.split(',') }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (entries.isEmpty() || entries.any { !it.contains('/') }) return emptyList()
        return entries.distinct()
    }
}
