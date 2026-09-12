package app.cicada.ui.addCredentialPage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.cicada.viewmodel.addCredential.AddCredentialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialScreen(
    onNavigateBack: () -> Unit,
    addCredentialViewModel: AddCredentialViewModel,
    credentialId: String? = null
) {
    val uiState by addCredentialViewModel.uiState.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(credentialId) {
        if(credentialId != null) {
            addCredentialViewModel.loadCredential(credentialId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if(uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (credentialId == null) "New Credential" else "Edit Credential") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    addCredentialViewModel.saveCredential()
                    println("Credential Saved to vault!!")
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                expanded = true,
                icon = { },
                text = { Text(if (credentialId == null) "Save to Vault" else "Update Vault") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.title,
                onValueChange = { addCredentialViewModel.updateTitle(it) },
                label = { Text("Title (e.g., Google, GitHub)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.username,
                onValueChange = { addCredentialViewModel.updateUsername(it) },
                label = { Text("Username or Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.usernameError != null,
                supportingText = {
                    uiState.usernameError?.let {
                        Text(it)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { addCredentialViewModel.updatePassword(it) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.passwordError != null,
                supportingText = {
                    uiState.passwordError?.let {
                        Text(it)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )

            OutlinedTextField(
                value = uiState.website,
                onValueChange = { addCredentialViewModel.updateWebsite(it) },
                label = { Text("Website (e.g. www.google.com, www.instagram.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.websiteError != null,
                supportingText = {
                    uiState.websiteError?.let {
                        Text(it)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { addCredentialViewModel.updateNotes(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        }
    }
}