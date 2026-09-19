package app.cicada.viewmodel.createUser

data class CreateUserUIState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val errorMsg: String? = null,

    val isCreating: Boolean = false,
    val isCreated: Boolean =  false
)