package org.dorossi.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Small persisted preferences. */
class SettingsRepository(private val context: Context) {

    /**
     * Platform address that worked last time. The account (activation, device id) is stored by the
     * page per address, so the app keeps using the same one as long as it answers.
     */
    val baseUrl: Flow<String?> = context.settingsDataStore.data.map { prefs -> prefs[BASE_URL] }

    suspend fun setBaseUrl(url: String) {
        context.settingsDataStore.edit { it[BASE_URL] = url }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
    }
}
