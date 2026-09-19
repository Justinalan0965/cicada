package app.cicada.viewmodel.createUser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.cicada.data.user.UserRepository

class CreateUserViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass : Class<T>
    ): T {
        if (modelClass.isAssignableFrom(CreateUserViewModel::class.java)) {
            return CreateUserViewModel(
                userRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}