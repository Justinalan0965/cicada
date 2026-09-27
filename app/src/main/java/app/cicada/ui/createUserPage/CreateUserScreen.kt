package app.cicada.ui.createUserPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import app.cicada.viewmodel.createUser.CreateUserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    onBackClick: () -> Unit,
    onUserCreated: () -> Unit,
    createUserViewModel: CreateUserViewModel
) {
    val uiState by createUserViewModel.uiState.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isCreated) {
        if (uiState.isCreated) {
            onUserCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // Leave empty, we will put the title in the main column
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Pushes the content slightly above center for a better visual balance
            verticalArrangement = Arrangement.Center
        ) {
            // Header Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Set up your master vault credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Username
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { createUserViewModel.updateUsername(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                singleLine = true,
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Master Password
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { createUserViewModel.updatePassword(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Master Password") },
                singleLine = true,
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide Password" else "Show Password"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm Password
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = { createUserViewModel.updateConfirmPassword(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm Password") },
                singleLine = true,
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide Password" else "Show Password"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = { createUserViewModel.createUser() },
                enabled = !uiState.isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (uiState.isCreating) "Creating..." else "Create Account",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Pushes everything up slightly so the keyboard doesn't immediately overlap the button
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}