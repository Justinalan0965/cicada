package app.cicada.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cicada.data.credential.CredentialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val credentialRepository: CredentialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())

    val uiState: StateFlow<HomeUIState> = _uiState.asStateFlow()

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        viewModelScope.launch {
            try {
                credentialRepository
                    .getCredentials()
                    .collect { credentials ->
                        _uiState.value = HomeUIState(
                            credentials,
                            isLoading = false
                        )
                    }
            } catch (e : Exception) {
                _uiState.value = HomeUIState(
                    emptyList(),
                    isLoading = false,
                    e.message
                )
            }
        }
    }
}