package app.cicada.viewmodel.createUser

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateUserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUserUIState())

    val uiState: StateFlow<CreateUserUIState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            usernameError = null
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null
        )
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = null
        )
    }

    fun createUser() {
        val state = _uiState.value

        var newState = state
        var hasError = false

        if (state.username.isBlank()) {
            newState = newState.copy(
                usernameError = "Username is required"
            )
            hasError = true
        }

        if (state.password.isBlank()) {
            newState = newState.copy(
                passwordError = "Master password is required"
            )
            hasError = true
        } else if (state.password.length < 12) {
            newState = newState.copy(
                passwordError = "Use at least 12 characters"
            )
            hasError = true
        }

        if (state.confirmPassword.isBlank()) {
            newState = newState.copy(
                confirmPasswordError = "Please confirm your password"
            )
            hasError = true
        } else if (state.confirmPassword != state.password) {
            newState = newState.copy(
                confirmPasswordError = "Passwords do not match"
            )
            hasError = true
        }

        _uiState.value = newState

        if (hasError) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isCreating = true,
                errorMsg = null
            )

            try {
                val username = state.username.trim().lowercase()

                if (userRepository.usernameExists(username)) {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        usernameError = "Username already exists"
                    )
                    return@launch
                }

                val password =
                    state.password.toCharArray()

                try {
                    userRepository.createUser(
                        username = username,
                        password = password
                    )
                } finally {
                    password.fill('\u0000')
                }

                _uiState.value = _uiState.value.copy(
                    isCreated = true,
                    isCreating = false
                )

            } catch (e: Exception) {
                Log.e("createUser", "Failed to create account", e)
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    isCreated = false,
                    errorMsg = "Failed to create account"
                )
            }
        }
    }
}