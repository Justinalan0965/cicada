package app.cicada.ui.createUserPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cicada.viewmodel.createUser.CreateUserViewModel

@Composable
fun CreateUserScreen(
    onUserCreated: () -> Unit,
    createUserViewModel: CreateUserViewModel
) {

    val uiState by createUserViewModel.uiState.collectAsState()

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(uiState.isCreated) {
        if (uiState.isCreated) {
            onUserCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // User name
        OutlinedTextField(
            value = uiState.username,
            onValueChange = {
                createUserViewModel.updateUsername(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Username")
            },
            singleLine = true,
            isError = uiState.usernameError != null,
            supportingText = {
                uiState.usernameError?.let{
                    Text(it)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        // Master password
        OutlinedTextField(
            value = uiState.password,
            onValueChange = {
                createUserViewModel.updatePassword(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            singleLine = true,
            isError = uiState.passwordError != null,
            supportingText = {
                uiState.passwordError?.let {
                    Text(it)
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Hide Password"
                        } else {
                            "Show Password"
                        }
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        // Confirm password
        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = {
                createUserViewModel.updateConfirmPassword(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Confirm password")
            },
            singleLine = true,
            isError = uiState.confirmPasswordError != null,
            supportingText = {
                uiState.confirmPasswordError?.let {
                    Text(it)
                }
            },
            visualTransformation = if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(
                    onClick = {
                        confirmPasswordVisible = !confirmPasswordVisible
                    }
                ) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (confirmPasswordVisible) {
                            "Hide Password"
                        } else {
                            "Show Password"
                        }
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                createUserViewModel.createUser()
            },
            enabled = !uiState.isCreating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.isCreating) {
                    "Creating"
                } else {
                    "Create Account"
                }
            )
        }
    }
}