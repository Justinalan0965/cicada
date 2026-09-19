package app.cicada.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.user.UserRepository
import app.cicada.security.VaultSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val vaultSession: VaultSession
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUIState())

    val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            usernameError = null,
            loginError = null
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null,
            loginError = null
        )
    }

    fun login() {

        val currentState = _uiState.value

        var hasError = false

        if (currentState.username.isBlank()) {
            _uiState.value = _uiState.value.copy(
                usernameError = "Username required"
            )
            hasError = true
        }

        if (currentState.password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                passwordError = "Password required"
            )
            hasError = true
        }

        if (hasError) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loginError = null
            )

            val password = currentState.password.toCharArray()

            try {

                val result = userRepository.unlockUser(
                    username = currentState.username,
                    password = password
                )

                if (result != null) {

                    try {
                        vaultSession.unlock(
                            result.userId,
                            result.vaultKey
                        )
                    } finally {
                        // VaultSession made its own copy
                        result.vaultKey.fill(0)
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginError = null,
                        isLoginSuccess = true
                    )

                } else {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginError = "Invalid username or password",
                        isLoginSuccess = false
                    )
                }

            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginError = "Login failed",
                    isLoginSuccess = false
                )

            } finally {
                password.fill('\u0000')
            }
        }
    }
}