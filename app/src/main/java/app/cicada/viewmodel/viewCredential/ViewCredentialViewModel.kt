package app.cicada.viewmodel.viewCredential

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.credential.CredentialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewCredentialViewModel(
    private val credentialRepository: CredentialRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(ViewCredentialUIState())

    val uiState: StateFlow<ViewCredentialUIState> = _uiState.asStateFlow()

    fun getCredentialDetails(credentialId: String) {
        viewModelScope.launch {
            try {
                val credential = credentialRepository.getCredential(credentialId)

                _uiState.value = _uiState.value.copy(
                    credential = credential,
                    isLoading = false
                )
            } catch (e : Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMsg = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun deleteCredential(credentialId: String) {
        viewModelScope.launch {
            try {
                credentialRepository.deleteCredential(credentialId)
                _uiState.value = _uiState.value.copy(
                    isDeleted = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleted = false,
                    errorMsg = "Failed to delete credential"
                )
            }
        }
    }
}