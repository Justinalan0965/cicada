package app.cicada.viewmodel.home

import app.cicada.data.credential.Credential

data class HomeUIState (
    val credentials : List<Credential> = emptyList(),
    val isLoading : Boolean = true,
    val errorMsg : String ?= null
)