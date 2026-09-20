package app.cicada.viewmodel.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.user.BiometricRepository
import app.cicada.data.user.UserRepository
import app.cicada.security.VaultSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class LoginViewModel(
    private val userRepository: UserRepository,
    private val vaultSession: VaultSession,
    private val biometricRepository: BiometricRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUIState())

    val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow()

    private var biometricUserId: String? = null
    private var biometricEncryptedVaultKey: ByteArray? = null

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            usernameError = null,
            loginError = null,
            biometricAvailable = false
        )

        checkBiometricAvailability(username)
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

                Log.d("LoginPage", "Failed to login:", e)

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

    fun biometricLogin(
        username: String,
        onAuthenticate: (Cipher) -> Unit,
        onUnavailable: () -> Unit
    ) {
        val normalizedUsername = username.trim().lowercase()

        if (normalizedUsername.isBlank()) {
            _uiState.value = _uiState.value.copy(
                usernameError = "Username required"
            )
            return
        }

        viewModelScope.launch {
            try {
                val biometricData =
                    biometricRepository.prepareBiometricUnlock(
                        normalizedUsername
                    )

                if (biometricData == null) {
                    onUnavailable()
                    return@launch
                }

                // Keep these only for the duration of the
                // biometric authentication operation.
                biometricUserId = biometricData.userId
                biometricEncryptedVaultKey =
                    biometricData.encryptedVaultKey.copyOf()

                val cipher =
                    biometricRepository.prepareDecryptionCipher(
                        userId = biometricData.userId,
                        encryptedVaultKey =
                            biometricData.encryptedVaultKey
                    )

                onAuthenticate(cipher)

            } catch (e: Exception) {
                android.util.Log.e(
                    "BiometricLogin",
                    "Failed to prepare biometric login",
                    e
                )

                clearBiometricState()
                onUnavailable()
            }
        }
    }

    fun completeBiometricLogin(
        authenticatedCipher: Cipher
    ) {
        val userId = biometricUserId
        val encryptedVaultKey = biometricEncryptedVaultKey

        if (userId == null || encryptedVaultKey == null) {
            _uiState.value = _uiState.value.copy(
                loginError = "Biometric unlock session expired",
                isLoginSuccess = false
            )
            return
        }

        viewModelScope.launch {
            try {
                val vaultKey =
                    biometricRepository.decryptVaultKey(
                        userId = userId,
                        encryptedVaultKey = encryptedVaultKey,
                        authenticatedCipher = authenticatedCipher
                    )

                try {
                    vaultSession.unlock(
                        userId = userId,
                        key = vaultKey
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loginError = null,
                        isLoginSuccess = true
                    )

                } finally {
                    vaultKey.fill(0)
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "BiometricLogin",
                    "Failed to decrypt vault key",
                    e
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loginError = "Biometric unlock failed",
                    isLoginSuccess = false
                )
            } finally {
                clearBiometricState()
            }
        }
    }

    private fun checkBiometricAvailability(
        username: String
    ) {
        val normalizedUsername = username.trim()

        if (normalizedUsername.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                val enabled =
                    biometricRepository.isBiometricEnabled(
                        normalizedUsername
                    )

                // Make sure the username hasn't changed while
                // the database query was running.
                if (
                    _uiState.value.username.trim()
                        .equals(normalizedUsername, ignoreCase = true)
                ) {
                    _uiState.value =
                        _uiState.value.copy(
                            biometricAvailable = enabled
                        )
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "BiometricLogin",
                    "Failed to check biometric availability",
                    e
                )
            }
        }
    }

    private fun clearBiometricState() {
        biometricEncryptedVaultKey?.fill(0)
        biometricEncryptedVaultKey = null
        biometricUserId = null
    }

    override fun onCleared() {
        clearBiometricState()
        super.onCleared()
    }
}