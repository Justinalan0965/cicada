package app.cicada.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cicada.data.vault.VaultRepository
import app.cicada.security.VaultSession

class LoginViewModelFactory(
    private val vaultRepository: VaultRepository,
    private val vaultSession: VaultSession
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                vaultRepository = vaultRepository,
                vaultSession = vaultSession
            ) as T
        }

        throw IllegalArgumentException(
            "Unknow viewModel class"
        )
    }
}