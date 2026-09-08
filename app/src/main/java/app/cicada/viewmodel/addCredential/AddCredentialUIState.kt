package app.cicada.viewmodel.addCredential

data class AddCredentialUIState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val notes: String = "",

    val isSaving: Boolean = false,
    val isSaved: Boolean = false,

    val usernameError: String? = null,
    val passwordError: String? = null,
    val websiteError: String? = null,
    val errorMsg: String? = null
)