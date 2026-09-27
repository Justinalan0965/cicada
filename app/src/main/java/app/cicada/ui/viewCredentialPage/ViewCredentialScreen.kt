package app.cicada.ui.viewCredentialPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cicada.security.CicadaClipboardManager
import app.cicada.ui.components.SiteIcon
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
    var menuExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
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

    // Moving Scaffold to the root ensures the TopAppBar is always visible,
    // allowing the user to navigate back even if the credential fails to load.
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.credential?.title ?: if (uiState.isLoading) "Loading..." else "Not Found",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                actions = {
                    // Only show Edit/Delete options if a valid credential was loaded
                    if (uiState.credential != null) {
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
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
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
                    }
                }
            )
        }
    ) { paddingValues ->

        // Handle Dialog separately from the main content flow
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Credential?") },
                text = { Text("Are you sure you want to permanently delete '${uiState.credential?.title ?: "this credential"}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewCredentialViewModel.deleteCredential(credentialId)
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.errorMsg != null -> {
                    Text(
                        text = "Error: ${uiState.errorMsg}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.credential == null -> {
                    Text(
                        text = "Credential not found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    val credential = uiState.credential!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Centered Visual Header
                        SiteIcon(
                            title = credential.title,
                            website = credential.website,
                            // You can modify your SiteIcon component to accept a size modifier
                            // modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Primary Credential Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                DetailField(
                                    label = "Username / Email",
                                    value = credential.username,
                                    onCopy = { clipboardManager.copy("Username", credential.username) },
                                    showDivider = true
                                )

                                // Password Field
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Text(
                                        text = "Password",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (passwordVisible) credential.password else "••••••••••••••••",
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = {
                                            clipboardManager.copyPassword(scope, credential.password)
                                        }) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy password",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Secondary Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                DetailField(
                                    label = "Website",
                                    value = credential.website?.ifBlank { "Not provided" } ?: "Not provided",
                                    onCopy = { clipboardManager.copy("Website", credential.website ?: "") },
                                    showDivider = true
                                )

                                DetailField(
                                    label = "Notes",
                                    value = credential.notes?.ifBlank { "No notes attached" } ?: "No notes attached",
                                    onCopy = { clipboardManager.copy("Notes", credential.notes ?: "") },
                                    showDivider = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailField(
    label: String,
    value: String,
    onCopy: () -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
           modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onCopy,
                // Disable the copy button if the field is empty
                enabled = value != "Not provided" && value != "No notes attached"
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = if (value != "Not provided" && value != "No notes attached")
                           MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}