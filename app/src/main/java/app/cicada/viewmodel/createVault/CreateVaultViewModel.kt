package app.cicada.viewmodel.createVault

import android.R.attr.password
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.vault.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateVaultViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateVaultUIState())

    val uiState: StateFlow<CreateVaultUIState> = _uiState.asStateFlow()

    fun updateVaultName(name: String) {
        _uiState.value = _uiState.value.copy(
            vaultName = name,
            vaultNameError = null
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

    fun createVault() {
        val state = _uiState.value

        var hasError = false

        if (state.vaultName.isBlank()) {
            _uiState.value = state.copy(
                vaultNameError = "Vault name is required"
            )

            hasError = true
        }

        if (state.password.isBlank()) {
            _uiState.value = state.copy(
                passwordError = "Master password is required"
            )

            hasError = true
        } else if (state.password.length < 12) {
            _uiState.value = state.copy(
                passwordError = "Use at least 12 characters"
            )

            hasError = true
        }

        if (state.confirmPassword.isBlank()) {
            _uiState.value = state.copy(
                confirmPasswordError = "Please confirm your password"
            )

            hasError = true
        } else if (state.confirmPassword != state.password) {
            _uiState.value = state.copy(
                confirmPasswordError = "Passwords do not match"
            )

            hasError = true
        }

        if (hasError) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isCreating = true,
                errorMsg = null
            )

            try {

                vaultRepository.createVault(
                    state.vaultName.trim(),
                    state.password.toCharArray()
                )

                _uiState.value = state.copy(
                    isCreated = true,
                    isCreating = false
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isCreating = false,
                    isCreated = false,
                    errorMsg = "Failed to create vault"
                )
            }
        }
    }
}