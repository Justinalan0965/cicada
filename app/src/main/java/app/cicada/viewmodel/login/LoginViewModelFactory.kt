package app.cicada.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cicada.data.user.UserRepository
import app.cicada.security.VaultSession

class LoginViewModelFactory(
    private val userRepository: UserRepository,
    private val vaultSession: VaultSession
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                userRepository = userRepository,
                vaultSession = vaultSession
            ) as T
        }

        throw IllegalArgumentException(
            "Unknow viewModel class"
        )
    }
}