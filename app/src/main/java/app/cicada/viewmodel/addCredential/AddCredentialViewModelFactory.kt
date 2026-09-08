package app.cicada.viewmodel.addCredential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cicada.data.credential.CredentialRepository

class AddCredentialViewModelFactory(
    private val credentialRepository: CredentialRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass : Class<T>
    ): T {
        if (modelClass.isAssignableFrom(AddCredentialViewModel::class.java)) {
            return AddCredentialViewModel(
                credentialRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}