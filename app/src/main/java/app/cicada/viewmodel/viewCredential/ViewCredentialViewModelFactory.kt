package app.cicada.viewmodel.viewCredential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cicada.data.credential.CredentialRepository

class ViewCredentialViewModelFactory(
    private val credentialRepository: CredentialRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(ViewCredentialViewModel::class.java)) {
            return ViewCredentialViewModel(
                credentialRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}