package app.cicada.ui.addCredentialPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
        if (credentialId != null) {
            addCredentialViewModel.loadCredential(credentialId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (credentialId == null) "New Credential" else "Edit Credential",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = {
                    Icon(Icons.Default.Save, contentDescription = null)
                },
                text = {
                    Text(
                        text = if (credentialId == null) "Save to Vault" else "Update Vault",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp) // Matched to 24.dp for consistency with Login/Create
                .verticalScroll(rememberScrollState()), // Crucial to prevent keyboard from hiding fields
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title Field
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { addCredentialViewModel.updateTitle(it) },
                label = { Text("Title (e.g., Google, GitHub)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Username Field
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { addCredentialViewModel.updateUsername(it) },
                label = { Text("Username or Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.usernameError != null,
                supportingText = uiState.usernameError?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Password Field
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { addCredentialViewModel.updatePassword(it) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
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
                    val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Website Field
            OutlinedTextField(
                value = uiState.website,
                onValueChange = { addCredentialViewModel.updateWebsite(it) },
                label = { Text("Website (e.g. www.google.com)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.websiteError != null,
                supportingText = uiState.websiteError?.let { error ->
                    { Text(text = error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Notes Field (Multiline)
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { addCredentialViewModel.updateNotes(it) },
                label = { Text("Notes (Recovery codes, etc.)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3, // Makes it visibly larger to indicate multi-line support
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default // Allows the enter key to create new lines
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Spacer to ensure the bottom field isn't covered by the Floating Action Button
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}