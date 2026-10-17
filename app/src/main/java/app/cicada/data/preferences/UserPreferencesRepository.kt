package app.cicada.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "cicada_preferences"
)

class UserPreferencesRepository(
    private val context: Context
) {

    companion object {
        private val LAST_USER_ID =
            stringPreferencesKey("last_user_id")

        private val AUTOFILL_AUTOSAVE_ENABLED =
            booleanPreferencesKey("autofill_autosave_enabled")

        // 1. Add the new key for the timeout
        private val AUTO_LOCK_TIMEOUT =
            longPreferencesKey("auto_lock_timeout")

        // Default to 5 minutes
        private const val DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L
    }

    val lastUserId: Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[LAST_USER_ID]
        }

    val autofillAutosaveEnabled: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[AUTOFILL_AUTOSAVE_ENABLED] ?: true
        }

    // 2. Expose the timeout as a Flow
    val autoLockTimeout: Flow<Long> =
        context.dataStore.data.map { preferences ->
            preferences[AUTO_LOCK_TIMEOUT] ?: DEFAULT_TIMEOUT_MS
        }

    suspend fun setLastUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_USER_ID] = userId
        }
    }

    suspend fun clearLastUserId() {
        context.dataStore.edit { preferences ->
            preferences.remove(LAST_USER_ID)
        }
    }

    suspend fun setAutofillAutosaveEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTOFILL_AUTOSAVE_ENABLED] = enabled
        }
    }

    // 3. Add a function to update the timeout from your Settings screen
    suspend fun setAutoLockTimeout(timeoutMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_LOCK_TIMEOUT] = timeoutMs
        }
    }
}