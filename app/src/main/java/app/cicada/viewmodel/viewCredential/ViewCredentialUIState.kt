package app.cicada.viewmodel.viewCredential

import app.cicada.data.credential.Credential

data class ViewCredentialUIState (
    val credential: Credential? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMsg: String? = null
)