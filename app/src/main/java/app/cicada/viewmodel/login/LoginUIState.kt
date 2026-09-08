package app.cicada.viewmodel.login

import app.cicada.data.vault.VaultInfo

data class LoginUIState(
    val vaults: List<VaultInfo> = emptyList(),
    val selectedVault: VaultInfo? = null,
    val password: String = "",
    val vaultError: String? = "",
    val passwordError: String? = "",
    val loginError: String? = "",
    val isLoading: Boolean = false,
    val isLoginSuccess: Boolean = false
)