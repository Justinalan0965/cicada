package app.cicada.viewmodel.login

import android.R.attr.password
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.database.CicadaDB
import app.cicada.data.vault.VaultInfo
import app.cicada.data.vault.VaultRepository
import app.cicada.security.CryptoManager
import app.cicada.security.VaultSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val vaultRepository: VaultRepository,
    private val vaultSession: VaultSession
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUIState())

    val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow()

    init {
        loadVaults()
    }

    private fun loadVaults() {

        viewModelScope.launch {
            vaultRepository
                .getVaults()
                .collect {
                    vaults ->

                    val currentlySelected = _uiState.value.selectedVault
                    val selectedVault = currentlySelected?.let {
                        selected ->

                        vaults.find{
                            it.id == selected.id
                        }
                    } ?: vaults.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        vaults = vaults,
                        selectedVault = selectedVault
                    )
                }
        }
    }

    fun selectVault(vault: VaultInfo) {
        _uiState.value = _uiState.value.copy(
            selectedVault = vault,
            vaultError = null,
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

    fun unlockVault() {

        val currentState = _uiState.value

        var hasError = false

        if (currentState.selectedVault == null) {
            _uiState.value = _uiState.value.copy(
                vaultError = "Select a vault"
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

                val vaultKey = vaultRepository.unlockVault(currentState.selectedVault!!.id, password)

                if (vaultKey != null) {

                    vaultSession.unlock(currentState.selectedVault!!.id, vaultKey)

                    vaultKey.fill(0)

                    _uiState.value = _uiState.value.copy(
                        loginError = null,
                        isLoginSuccess = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        loginError = "Invalid password",
                        isLoginSuccess = false,
                        isLoading = false
                    )
                }
            } finally {
                password.fill('\u0000')
            }
        }
    }
}