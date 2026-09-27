package app.cicada.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
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
    }

    val lastUserId: Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[LAST_USER_ID]
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
}