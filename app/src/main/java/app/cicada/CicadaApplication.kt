
package app.cicada

import android.app.Application
import app.cicada.data.preferences.UserPreferencesRepository
import app.cicada.security.VaultSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CicadaApplication : Application() {

    val vaultSession: VaultSession by lazy {
        VaultSession()
    }

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var userPreferencesRepository:
            UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()

        userPreferencesRepository =
            UserPreferencesRepository(applicationContext)

        applicationScope.launch {
            userPreferencesRepository.autoLockTimeout.collect {
                    timeoutMs ->
                vaultSession.autoLockTimeoutMs = timeoutMs
            }
        }
    }
}
