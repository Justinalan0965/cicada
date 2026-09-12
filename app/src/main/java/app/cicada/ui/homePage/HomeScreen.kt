package app.cicada.ui.homePage


import android.R.attr.onClick
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.cicada.data.credential.Credential
import app.cicada.security.CicadaClipboardManager
import app.cicada.ui.components.SiteIcon
import app.cicada.viewmodel.home.HomeViewModel
import kotlinx.coroutines.launch
import java.util.UUID

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    Vault(route = "home", label = "Home", icon = Icons.Default.Home, contentDescription = "Home page"),
    Settings(route = "settings", label = "Settings", icon = Icons.Default.Settings, contentDescription = "App Settings")
}

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToAdd: () -> Unit,
    currentRoute: String?,
    onNavigateBottomBar: (String) -> Unit,
    onClick: (String) -> Unit
) {

    val uiState by homeViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search vault...")},
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search icon")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape

                )
            }
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { onNavigateBottomBar(destination.route) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.contentDescription
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Credential"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.errorMsg != null -> {
                    Text(
                        text = "Error: ${uiState.errorMsg}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    CredentialList(uiState.credentials, onClick)
                }
            }
        }
    }
}

@Composable
fun CredentialList(
    credentials: List<Credential>,
    onClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(credentials) { credential ->
            CredentialCard(credential, onClick)
        }
    }
}

@Composable
fun CredentialCard(
    credential: Credential,
    onClick: (String) -> Unit
) {

    Card(
        onClick = {
            onClick(credential.id)
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                SiteIcon(
                    title = credential.title,
                    website = credential.website
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = credential.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = credential.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

/*                IconButton(onClick = {
                    clipboardManager.copy("Username", credential.username)
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy username",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }*/
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun viewCards() {
    val context = LocalContext.current

    val credentials: List<Credential> = listOf(
        Credential(UUID.randomUUID().toString(), "Google", "highgin@gmail.com", "no high enough", "www.google.com", null),
        Credential(UUID.randomUUID().toString(), "Instagram", "highgin@gmail.com", "no high enough", "www.instgram.com", null),
        Credential(UUID.randomUUID().toString(), "Facebook", "highgin@gmail.com", "no high enough", "www.facebook.com", null),
        Credential(UUID.randomUUID().toString(), "Reddit", "highgin@gmail.com", "no high enough", "www.reddit.com", null)
    )

    CredentialList(
        credentials,
        onClick = { credentialId ->
            println("Clicked: $credentialId")
        }
    )
}