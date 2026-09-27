package app.cicada.viewmodel.login

data class LoginUIState(
    val username: String = "",
    val password: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val biometricAvailable: Boolean = false,
    val rememberedUsername: String? = null,
    val rememberedUserId: String? = null,
    val hasAccounts: Boolean = false,
    val selectedUserId: String? = null
)