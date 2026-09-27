package app.cicada.viewmodel.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.preferences.UserPreferencesRepository
import app.cicada.data.user.BiometricRepository
import app.cicada.data.user.UserEntity
import app.cicada.data.user.UserRepository
import app.cicada.security.VaultSession
import app.cicada.security.exception.BiometricKeyInvalidatedException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.Cipher

class LoginViewModel(
    private val userRepository: UserRepository,
    private val vaultSession: VaultSession,
    private val biometricRepository: BiometricRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUIState())
    val uiState: StateFlow<LoginUIState> = _uiState.asStateFlow()

    private val _rememberedUser = MutableStateFlow<UserEntity?>(null)
    val rememberedUser: StateFlow<UserEntity?> = _rememberedUser.asStateFlow()

    private val _availableUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val availableUsers: StateFlow<List<UserEntity>> = _availableUsers.asStateFlow()

    private var biometricUserId: String? = null
    private var biometricEncryptedVaultKey: ByteArray? = null

    private var biometricRequestId = 0L

    private var autoBiometricAttempted = false

    fun updateUsername(username: String) {
        _uiState.update {
            it.copy(
                username = username,
                usernameError = null,
                loginError = null
            )
        }

        checkBiometricAvailability(username)
    }

    fun updatePassword(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                loginError = null
            )
        }
    }

    fun login() {

        val currentState = _uiState.value

        val username = currentState.username.trim()
        val password = currentState.password

        var hasError = false

        if (username.isBlank()) {
            _uiState.update {
                it.copy(
                    usernameError = "Username is required",
                    loginError = null
                )
            }
            hasError = true
        } else {
            _uiState.update {
                it.copy(usernameError = null)
            }
        }

        if (password.isBlank()) {
            _uiState.update {
                it.copy(
                    passwordError = "Password is required",
                    loginError = null
                )
            }
            hasError = true
        } else {
            _uiState.update {
                it.copy(passwordError = null)
            }
        }

        if (hasError) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                loginError = null
            )
        }

        viewModelScope.launch {

            val passwordChars = password.toCharArray()

            try {

                val result = userRepository.unlockUser(
                    username = username,
                    password = passwordChars
                )

                if (result == null) {

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginError = "Invalid username or password",
                            isLoginSuccess = false
                        )
                    }

                    return@launch
                }


                vaultSession.unlock(
                    userId = result.userId,
                    key = result.vaultKey
                )

                userPreferencesRepository.setLastUserId(
                    result.userId
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginError = null,
                        isLoginSuccess = true
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "Login",
                    "Login failed",
                    e
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginError = "Login failed",
                        isLoginSuccess = false
                    )
                }

            } finally {
                passwordChars.fill('\u0000')
            }
        }
    }

    fun biometricLogin(
        username: String,
        onAuthenticate: (Cipher, Long) -> Unit,
        onUnavailable: () -> Unit
    ) {

        val normalizedUsername = username.trim().lowercase()

        if (normalizedUsername.isBlank()) {

            _uiState.update {
                it.copy(
                    usernameError = "Username is required",
                    loginError = null
                )
            }

            return
        }

        viewModelScope.launch {

            val user = userRepository.getUserByUsername(
                normalizedUsername
            )

            if (user == null) {

                _uiState.update {
                    it.copy(
                        loginError = "User not found"
                    )
                }

                onUnavailable()
                return@launch
            }

            biometricLoginForUserId(
                userId = user.id,
                onAuthenticate = onAuthenticate,
                onUnavailable = onUnavailable
            )
        }
    }

    fun biometricLoginForRememberedUser(
        onAuthenticate: (Cipher, Long) -> Unit,
        onUnavailable: () -> Unit
    ) {

        val userId = _uiState.value.rememberedUserId

        if (userId == null) {
            onUnavailable()
            return
        }

        biometricLoginForUserId(
            userId = userId,
            onAuthenticate = onAuthenticate,
            onUnavailable = onUnavailable
        )
    }

    private fun biometricLoginForUserId(
        userId: String,
        onAuthenticate: (Cipher, Long) -> Unit,
        onUnavailable: () -> Unit
    ) {

        biometricRequestId++

        val currentRequestId = biometricRequestId

        viewModelScope.launch {

            try {

                val biometricData =
                    biometricRepository.prepareBiometricUnlockByUserId(
                        userId
                    )

                if (biometricData == null) {

                    clearBiometricState()

                    onUnavailable()

                    return@launch
                }

                biometricUserId = biometricData.userId

                biometricEncryptedVaultKey = biometricData.encryptedVaultKey.copyOf()

                val cipher = biometricRepository.prepareDecryptionCipher(
                        biometricData.userId,
                        biometricData.encryptedVaultKey
                    )

                onAuthenticate(
                    cipher,
                    currentRequestId
                )

            } catch (e: BiometricKeyInvalidatedException) {
                Log.d(
                    "BiometricLogin",
                    "Biometric key invalidated",
                    e
                )

                clearBiometricState()

                try {
                    biometricRepository.invalidateBiometric(userId)
                } catch (cleanupException: Exception) {

                    Log.e(
                        "BiometricLogin",
                        "Failed to invalidate biometric data",
                        cleanupException
                    )
                }

                _uiState.update {
                    it.copy(
                        biometricAvailable = false,
                        loginError = "Biometric unlock is no longer available"
                    )
                }

                onUnavailable()

            } catch (e: Exception) {

                Log.e(
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
        authenticatedCipher: Cipher,
        requestId: Long
    ) {

        if (requestId != biometricRequestId) {
            return
        }

        val userId = biometricUserId
            ?: return

        val encryptedVaultKey =
            biometricEncryptedVaultKey
                ?: return

        viewModelScope.launch {

            try {

                val vaultKey =
                    biometricRepository.decryptVaultKey(
                        userId = userId,
                        encryptedVaultKey = encryptedVaultKey,
                        authenticatedCipher = authenticatedCipher
                    )

                if (vaultSession.isUnlocked) {

                    vaultKey.fill(0)

                    clearBiometricState()

                    return@launch
                }

                try {

                    vaultSession.unlock(
                        userId = userId,
                        key = vaultKey
                    )

                    userPreferencesRepository.setLastUserId(
                        userId
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginError = null,
                            isLoginSuccess = true
                        )
                    }

                } finally {
                    vaultKey.fill(0)
                }

            } catch (e: Exception) {

                Log.e(
                    "BiometricLogin",
                    "Biometric unlock failed",
                    e
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginError = "Biometric unlock failed",
                        isLoginSuccess = false
                    )
                }

            } finally {

                clearBiometricState()
            }
        }
    }

    fun cancelBiometricLogin() {

        biometricRequestId++

        clearBiometricState()
    }

    fun checkBiometricAvailability(
        username: String
    ) {

        val normalizedUsername =
            username.trim().lowercase()

        if (normalizedUsername.isBlank()) {

            _uiState.update {
                it.copy(
                    biometricAvailable = false
                )
            }

            return
        }

        viewModelScope.launch {

            try {

                val available =
                    biometricRepository.isBiometricEnabled(
                        normalizedUsername
                    )

                _uiState.update {
                    it.copy(
                        biometricAvailable = available
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "BiometricLogin",
                    "Failed to check biometric availability",
                    e
                )

                _uiState.update {
                    it.copy(
                        biometricAvailable = false
                    )
                }
            }
        }
    }

    fun loadRememberedUser() {
        viewModelScope.launch {

            val users = userRepository.getAllUsers()

            _availableUsers.value = users

            if (users.isEmpty()) {
                _rememberedUser.value = null

                _uiState.update {
                    it.copy(
                        hasAccounts = false,
                        rememberedUsername = null,
                        rememberedUserId = null,
                        biometricAvailable = false,
                        username = ""
                    )
                }

                return@launch
            }

            _uiState.update {
                it.copy(hasAccounts = true)
            }

            val userId = userPreferencesRepository.lastUserId.first()

            if (userId == null) {
                _rememberedUser.value = null

                _uiState.update {
                    it.copy(
                        rememberedUsername = null,
                        rememberedUserId = null,
                        biometricAvailable = false
                    )
                }

                return@launch
            }

            val user = userRepository.getUserById(userId)

            if (user == null) {
                userPreferencesRepository.clearLastUserId()

                _rememberedUser.value = null

                _uiState.update {
                    it.copy(
                        rememberedUsername = null,
                        rememberedUserId = null,
                        selectedUserId = null,
                        biometricAvailable = false
                    )
                }

                return@launch
            }

            _rememberedUser.value = user

            val biometricEnabled =
                biometricRepository.isBiometricEnabled(user.username)

            _uiState.update {
                it.copy(
                    rememberedUsername = user.username,
                    rememberedUserId = user.id,
                    selectedUserId = user.id,
                    username = user.username,
                    biometricAvailable = biometricEnabled
                )
            }
        }
    }

    fun tryAutomaticBiometricLogin(
        onAuthenticate: (Cipher, Long) -> Unit,
        onUnavailable: () -> Unit
    ) {

        if (autoBiometricAttempted) {
            return
        }

        val userId =
            _uiState.value.rememberedUserId
                ?: return

        if (!_uiState.value.biometricAvailable) {
            return
        }

        autoBiometricAttempted = true

        biometricLoginForUserId(
            userId = userId,
            onAuthenticate = onAuthenticate,
            onUnavailable = onUnavailable
        )
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

    fun selectAccount(
        user: UserEntity,
        onAuthenticate: (Cipher, Long) -> Unit,
        onUnavailable: () -> Unit
    ) {
        _uiState.update {
            it.copy(
                selectedUserId = user.id,
                username = user.username,
                password = "",
                usernameError = null,
                passwordError = null,
                loginError = null,
                biometricAvailable = false
            )
        }

        viewModelScope.launch {
            try {
                val biometricEnabled =
                    biometricRepository.isBiometricEnabled(user.username)

                _uiState.update {
                    it.copy(
                        biometricAvailable = biometricEnabled
                    )
                }

                if (biometricEnabled) {
                    biometricLoginForUserId(
                        userId = user.id,
                        onAuthenticate = onAuthenticate,
                        onUnavailable = onUnavailable
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "AccountSelection",
                    "Failed to check biometric availability",
                    e
                )

                _uiState.update {
                    it.copy(
                        biometricAvailable = false
                    )
                }
            }
        }
    }
}