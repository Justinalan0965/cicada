package app.cicada.viewmodel.createVault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cicada.data.vault.VaultRepository

class CreateVaultViewModelFactory(
    private val vaultRepository: VaultRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass : Class<T>
    ): T {
        if (modelClass.isAssignableFrom(CreateVaultViewModel::class.java)) {
            return CreateVaultViewModel(
                vaultRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}