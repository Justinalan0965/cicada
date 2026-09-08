package app.cicada.viewmodel.createVault

data class CreateVaultUIState(
    val vaultName: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    val vaultNameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMsg: String? = null,

    val isCreating: Boolean = false,
    val isCreated: Boolean =  false
)