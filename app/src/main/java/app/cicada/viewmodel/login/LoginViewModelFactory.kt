package app.cicada.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cicada.data.preferences.UserPreferencesRepository
import app.cicada.data.user.BiometricRepository
import app.cicada.data.user.UserRepository
import app.cicada.security.VaultSession

class LoginViewModelFactory(
    private val userRepository: UserRepository,
    private val vaultSession: VaultSession,
    private val biometricRepository: BiometricRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                userRepository = userRepository,
                vaultSession = vaultSession,
                biometricRepository = biometricRepository,
                userPreferencesRepository = userPreferencesRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknow viewModel class"
        )
    }
}