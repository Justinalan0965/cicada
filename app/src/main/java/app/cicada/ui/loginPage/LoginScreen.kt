package app.cicada.ui.loginPage

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cicada.viewmodel.login.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {

    val uiState by loginViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
        }
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "CICADA",
            fontSize = 35.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Your Passwords. Protected.",
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // Username
        OutlinedTextField(
            value = uiState.username,
            onValueChange = {
                loginViewModel.updateUsername(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Username")
            },
            singleLine = true,
            isError = uiState.usernameError != null,
            supportingText = {
                uiState.usernameError?.let {
                    Text(it)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // Master Password
        OutlinedTextField(
            value = uiState.password,
            onValueChange = {
                loginViewModel.updatePassword(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Master Password")
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

        uiState.loginError?.let {
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(it)
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = {
                loginViewModel.login()
            },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.isLoading) {
                    "Logging in..."
                } else {
                    "Login"
                }
            )
        }

        TextButton(
            onClick = onCreateAccount,
            enabled = !uiState.isLoading
        ) {
            Text("Create Account")
        }
    }
}