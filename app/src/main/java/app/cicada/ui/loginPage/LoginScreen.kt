package app.cicada.ui.loginPage

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cicada.security.BiometricAuthenticator
import app.cicada.viewmodel.login.LoginViewModel
import javax.crypto.Cipher

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    onBiometricLogin: (Cipher, Long) -> Unit,
    biometricAuthenticator: BiometricAuthenticator,
    loginViewModel: LoginViewModel = viewModel()
) {

    val uiState by loginViewModel.uiState.collectAsState()

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var showAccountPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loginViewModel.loadRememberedUser()
    }

    LaunchedEffect(
        uiState.rememberedUserId,
        uiState.biometricAvailable
    ) {

        if (
            uiState.rememberedUserId != null &&
            uiState.selectedUserId == uiState.rememberedUserId &&
            uiState.biometricAvailable &&
            biometricAuthenticator.canAuthenticate()
        ) {
            loginViewModel.tryAutomaticBiometricLogin(
                onAuthenticate = { cipher, requestId ->
                    biometricAuthenticator.authenticate(
                        cipher = cipher,
                        onSuccess = { authenticatedCipher ->
                            loginViewModel.completeBiometricLogin(
                                authenticatedCipher,
                                requestId
                            )
                        },
                        onFailure = { error ->
                            loginViewModel.cancelBiometricLogin()

                            Log.d(
                                "BiometricLogin",
                                "Automatic authentication failed: $error"
                            )
                        }
                    )
                },
                onUnavailable = {
                    Log.d(
                        "BiometricLogin",
                        "Automatic biometric unlock unavailable"
                    )
                }
            )
        }
    }

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
        }
    }


    Scaffold { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {
            // Header Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "CICADA",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Your Passwords. Protected.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }


            Spacer(modifier = Modifier.height(40.dp))

            // Username Field
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

                supportingText = uiState.usernameError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),

                shape = RoundedCornerShape(12.dp)
            )


            Spacer(modifier = Modifier.height(8.dp))

            // Master Password Field
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

                supportingText = uiState.passwordError?.let { error ->
                    {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },

                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),

                trailingIcon = {

                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (passwordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },

                            contentDescription =
                                if (passwordVisible) {
                                    "Hide Password"
                                } else {
                                    "Show Password"
                                }
                        )
                    }
                },

                shape = RoundedCornerShape(12.dp)
            )

            // General Login Error
            uiState.loginError?.let {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }


            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = {
                    loginViewModel.login()
                },

                enabled = !uiState.isLoading,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),

                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text =
                        if (uiState.isLoading) {
                            "Logging in..."
                        } else {
                            "Login"
                        },

                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Biometric Login Button (Conditionally Rendered)
            if (uiState.biometricAvailable) {

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                FilledTonalButton(

                    onClick = {
                        loginViewModel.biometricLogin(
                            username = uiState.username,

                            onAuthenticate = { cipher, requestId ->

                                onBiometricLogin(
                                    cipher,
                                    requestId
                                )
                            },

                            onUnavailable = {}
                        )
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    shape = RoundedCornerShape(12.dp)
                ) {

                    Icon(
                        imageVector = Icons.Default.Fingerprint,

                        contentDescription = "Biometrics",

                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier.size(
                            ButtonDefaults.IconSpacing
                        )
                    )

                    Text(
                        text = "Unlock with Biometrics",

                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create Account Link
            if (!uiState.hasAccounts) {
                TextButton(
                    onClick = onCreateAccount,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = "Don't have an account? Create one",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                TextButton(
                    onClick = {
                        showAccountPicker = true
                    },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = "Switch Account",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(32.dp)
            )

            if (showAccountPicker) {
                AlertDialog(
                    onDismissRequest = {
                        showAccountPicker = false
                    },
                    title = {
                        Text("Switch Account")
                    },
                    text = {
                        Column {
                            val users by loginViewModel.availableUsers.collectAsState()

                            users.forEach { user ->
                                TextButton(
                                    onClick = {
                                        showAccountPicker = false

                                        loginViewModel.selectAccount(
                                            user = user,

                                            onAuthenticate = { cipher, requestId ->
                                                biometricAuthenticator.authenticate(
                                                    cipher = cipher,
                                                    onSuccess = { authenticatedCipher ->
                                                        loginViewModel.completeBiometricLogin(
                                                            authenticatedCipher,
                                                            requestId
                                                        )
                                                    },
                                                    onFailure = { error ->
                                                        loginViewModel.cancelBiometricLogin()

                                                        Log.d(
                                                            "BiometricLogin",
                                                            "Account selection biometric authentication failed: $error"
                                                        )
                                                    }
                                                )
                                            },

                                            onUnavailable = {
                                                Log.d(
                                                    "BiometricLogin",
                                                    "Biometric unlock unavailable for selected account"
                                                )
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = user.username,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    showAccountPicker = false
                                    onCreateAccount()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "＋ Add Account",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showAccountPicker = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

        }
    }
}