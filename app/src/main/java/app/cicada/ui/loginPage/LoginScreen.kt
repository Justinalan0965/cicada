package app.cicada.ui.loginPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cicada.viewmodel.login.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onCreateVault: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {

    val uiState by loginViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
        }
    }

    var vaultMenuExpanded by remember {
        mutableStateOf(false)
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

        if (!uiState.vaults.isEmpty()) {
            Text(
                text = "CICADA",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Your Passwords. Protected."
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // Vault Selector
            ExposedDropdownMenuBox(
                expanded = vaultMenuExpanded,
                onExpandedChange = {
                    vaultMenuExpanded = !vaultMenuExpanded
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.selectedVault?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = {
                        Text("Vault")
                    },
                    isError = uiState.vaultError != null,
                    supportingText = {
                        uiState.vaultError?.let {
                            Text(it)
                        }
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = vaultMenuExpanded
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )

                ExposedDropdownMenu(
                    expanded = vaultMenuExpanded,
                    onDismissRequest = {
                        vaultMenuExpanded = false
                    }
                ) {
                    uiState.vaults.forEach { vault ->
                        DropdownMenuItem(
                            text = {
                                Text(vault.name)
                            },
                            onClick = {
                                loginViewModel.selectVault(vault)
                                vaultMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // Password
            OutlinedTextField(
                value = uiState.password,
                onValueChange = {
                    loginViewModel.updatePassword(it)
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

            uiState.loginError?.let {
                Text(it)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    loginViewModel.unlockVault()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (uiState.isLoading) {
                        "Unlocking"
                    } else {
                        "Unlock Vault"
                    }
                )
            }

            TextButton(
                onClick = onCreateVault
            ) {
                Text("Create New Vault")
            }
        } else {
            Spacer(modifier = Modifier.height(26.dp))

            Text("No vaults found")

            Spacer(modifier = Modifier.height(16.dp))

            Text("Create a vault and get started")

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Vault")
            }
        }
    }
}


