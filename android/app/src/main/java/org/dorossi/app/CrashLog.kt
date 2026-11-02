package org.dorossi.app

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Keeps the last crash on the phone so the next launch can show it and the user can send it.
 * Installed as early as possible (Application.attachBaseContext), before any library starts.
 */
object CrashLog {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val filesDir = context.filesDir
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val report = buildString {
                    append("DOROSSI ").append(BuildConfig.VERSION_NAME)
                    append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
                    append("Android ").append(Build.VERSION.RELEASE).append(" / SDK ").append(Build.VERSION.SDK_INT).append('\n')
                    append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                    append("Thread: ").append(thread.name).append("\n\n")
                    append(error.stackTraceToString())
                }
                File(filesDir, FILE_NAME).writeText(report.take(MAX_LENGTH))
            } catch (ignored: Throwable) {
                // Never let the crash log itself hide the original crash.
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The report of the last crash, removed once read; null when the app did not crash. */
    fun take(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        val text = try {
            file.readText()
        } catch (e: java.io.IOException) {
            null
        }
        file.delete()
        return text?.takeIf { it.isNotBlank() }
    }

    private const val MAX_LENGTH = 20_000
}
