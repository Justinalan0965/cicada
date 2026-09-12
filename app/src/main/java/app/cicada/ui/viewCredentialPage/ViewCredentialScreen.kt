package app.cicada.ui.viewCredentialPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cicada.security.CicadaClipboardManager
import app.cicada.viewmodel.viewCredential.ViewCredentialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewCredentialScreen(
    viewCredentialViewModel: ViewCredentialViewModel,
    clipboardManager: CicadaClipboardManager,
    credentialId: String,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit
) {

    var menuExpanded by remember{ mutableStateOf(false) }
    var passwordVisible by remember{ mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val uiState by viewCredentialViewModel.uiState.collectAsState()

    LaunchedEffect(credentialId) {
        viewCredentialViewModel.getCredentialDetails(credentialId)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.errorMsg != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${uiState.errorMsg}")
            }
        }

        uiState.credential == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Credential not found :(")
            }
        }

        else -> {

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(uiState.credential?.title ?: "") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            }

                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showDeleteDialog = false
                                    },
                                    title = {
                                        Text("Delete Credential?")
                                    },
                                    text = {
                                        Text("Are you sure you want delete ${uiState.credential?.title?: "this credential"}?")
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showDeleteDialog = false
                                                viewCredentialViewModel.deleteCredential(credentialId)
                                            }
                                        ) {
                                            Text("Delete")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showDeleteDialog = false
                                            }
                                        ) {
                                            Text("Cancel")
                                        }
                                    }
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    DetailField(
                        label = "Username / Email",
                        value = uiState.credential?.username ?: "",
                        onCopy = {
                            clipboardManager.copy(
                                "Username",
                                uiState.credential?.username ?: ""
                            )
                        }
                    )

                    Column {
                        Text(
                            "Password",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (passwordVisible) uiState.credential?.password
                                    ?: "" else "••••••••••••••••",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }

                            IconButton(onClick = {
                                clipboardManager.copyPassword(
                                    scope,
                                    uiState.credential?.password ?: ""
                                )

                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy password"
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    }

                    DetailField(
                        label = "Website",
                        value = uiState.credential?.website ?: "",
                        onCopy = {
                            clipboardManager.copy(
                                "Website",
                                uiState.credential?.website ?: ""
                            )
                        }
                    )

                    DetailField(
                        label = "Notes",
                        value = uiState.credential?.notes ?: "",
                        onCopy = {
                            clipboardManager.copy(
                                "Notes",
                                uiState.credential?.notes ?: ""
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailField(label: String, value: String, onCopy: () -> Unit) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
           modifier = Modifier
               .fillMaxWidth()
               .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label")
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}