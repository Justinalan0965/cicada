package app.cicada.viewmodel.addCredential

import android.util.Patterns
import androidx.compose.runtime.Composable
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

    fun validateWebsiteURL(url : String): Boolean {
        if (url.isBlank()) {
            return true
        }

        return Patterns.WEB_URL.matcher(url.trim()).matches()
    }

    fun addCredential() {

        val state = _uiState.value

        var hasError = false

        if (state.username.isBlank()) {
            _uiState.value = state.copy(
                usernameError = "username name cannot be empty"
            )
            hasError = true
        }

        if (state.password.isBlank()) {
            _uiState.value = state.copy(
                passwordError = "password cannot be empty"
            )
            hasError = true
        }

        if (!validateWebsiteURL(state.website)) {
            _uiState.value = _uiState.value.copy(
                websiteError = "Enter a valid website"
            )
            hasError = true
        }

        if (hasError) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isSaving =  true,
                errorMsg = null
            )

            try {

                credentialRepository.addCredentialToVault(
                    state.title,
                    state.username,
                    state.password,
                    state.website,
                    state.notes
                )

                _uiState.value = state.copy(
                    isSaved = true,
                    isSaving = false
                )
            } catch (e : Exception) {
                _uiState.value = state.copy(
                    isSaved = false,
                    isSaving = false,
                    errorMsg = "Failed to add credential"
                )
            }
        }
    }
}