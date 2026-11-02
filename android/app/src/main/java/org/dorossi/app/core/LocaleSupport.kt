package org.dorossi.app.core

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** In-app language selection (works on all Android versions through AppCompat). */
object LocaleSupport {
    /** Language tags the app ships strings for. The first one is the default (res/values). */
    val supportedTags: List<String> = listOf("ar-MA", "fr-MA")

    private val names: Map<String, String> = mapOf("ar" to "العربية", "fr" to "Français")

    fun displayName(tag: String): String = names[tag.substringBefore('-')] ?: tag

    /** The language currently in use, as one of [supportedTags]. */
    fun currentTag(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val language = if (appLocales.isEmpty) {
            Resources.getSystem().configuration.locales[0].language
        } else {
            appLocales[0]?.language
        }
        return supportedTags.firstOrNull { it.substringBefore('-') == language } ?: supportedTags.first()
    }

    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /**
     * When the phone's language is not one the app supports, switch the app to its default
     * language; otherwise the default strings would be shown with the wrong layout direction.
     */
    fun ensureSupportedLocale() {
        if (!AppCompatDelegate.getApplicationLocales().isEmpty) return
        val deviceLanguage = Resources.getSystem().configuration.locales[0].language
        if (supportedTags.none { it.substringBefore('-') == deviceLanguage }) {
            setLanguage(supportedTags.first())
        }
    }
}
