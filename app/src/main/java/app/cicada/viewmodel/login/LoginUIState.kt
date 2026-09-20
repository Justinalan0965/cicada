package app.cicada.viewmodel.login

data class LoginUIState(
    val username: String = "",
    val password: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false,
    val biometricAvailable: Boolean = false
)