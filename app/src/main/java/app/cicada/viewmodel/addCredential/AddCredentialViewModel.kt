package app.cicada.viewmodel.addCredential

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.credential.CredentialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddCredentialViewModel(
    private val credentialRepository : CredentialRepository
) : ViewModel() {

    private var editingCredentialId: String? = null
    private val _uiState = MutableStateFlow(AddCredentialUIState())

    val uiState : StateFlow<AddCredentialUIState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(
            title = title,
        )
    }

    fun updateUsername(username : String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            usernameError = null
        )
    }

    fun updatePassword(password : String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null
        )
    }

    fun updateWebsite(website : String) {
        _uiState.value = _uiState.value.copy(
            website = website,
            websiteError = null
        )
    }

    fun updateNotes(notes : String) {
        _uiState.value = _uiState.value.copy(
            notes = notes
        )
    }

    private fun validateWebsiteURL(url : String): Boolean {
        if (url.isBlank()) {
            return true
        }

        return Patterns.WEB_URL.matcher(url.trim()).matches()
    }

    fun saveCredential() {

        val state = _uiState.value

        var newState = state

        var hasError = false

        if (state.username.isBlank()) {
            newState = newState.copy(
                usernameError = "Username cannot be empty"
            )
            hasError = true
        }

        if (state.password.isBlank()) {
            newState = newState.copy(
                passwordError = "Password cannot be empty"
            )
            hasError = true
        }

        if (!validateWebsiteURL(state.website)) {
            newState = newState.copy(
                websiteError = "Enter a valid website"
            )
            hasError = true
        }

        _uiState.value = newState

        if (hasError) {
            return
        }

        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMsg = null
            )

            try {
                val credentialId = editingCredentialId

                if (credentialId == null) {
                    credentialRepository.addCredentialToVault(
                        state.title,
                        state.username,
                        state.password,
                        state.website,
                        state.notes
                    )
                } else {
                    credentialRepository.updateCredential(
                        credentialId = credentialId,
                        title = state.title,
                        username = state.username,
                        password = state.password,
                        website = state.website,
                        notes = state.notes
                    )
                }


                _uiState.value = _uiState.value.copy(
                    isSaved = true,
                    isSaving = false
                )
            } catch (e : Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaved = false,
                    isSaving = false,
                    errorMsg = "Failed to add credential"
                )
            }
        }
    }

    fun loadCredential(credentialId: String) {

        if (editingCredentialId == credentialId) {
            return
        }

        editingCredentialId = credentialId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMsg = null
            )

            try {
                val credential = credentialRepository.getCredential(credentialId)

                if (credentialId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMsg = "Credential not found"
                    )

                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    title = credential.title,
                    username = credential.username,
                    password = credential.password,
                    website = credential.website ?: "",
                    notes = credential.notes ?: "",
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMsg = "Failed to load credential"
                )
            }
        }
    }
}