package org.dorossi.app.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads and writes text in files the user picked (Storage Access Framework). */
object FileIo {
    suspend fun writeText(context: Context, uri: Uri, text: String) {
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val stream = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
                ?: resolver.openOutputStream(uri)
                ?: throw IOException("Cannot open $uri for writing")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }

    suspend fun readText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri for reading")
        val text = stream.use { String(it.readBytes(), Charsets.UTF_8) }
        text.removePrefix("\uFEFF")
    }
}
