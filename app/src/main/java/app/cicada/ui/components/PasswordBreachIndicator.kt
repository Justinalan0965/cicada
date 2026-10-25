
package app.cicada.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import app.cicada.security.PasswordBreachChecker
import app.cicada.security.PasswordBreachResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun PasswordBreachIndicator(
    password: String,
    website: String? = null,
    modifier: Modifier = Modifier,
    autoCheck: Boolean = false
) {
    var result by remember { mutableStateOf<PasswordBreachResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var requestId by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun performCheck(
        passwordToCheck: String,
        request: Int
    ) {
        if (passwordToCheck.isEmpty()) return

        isChecking = true
        result = null

        try {
            val response = PasswordBreachChecker.check(passwordToCheck)

            if (requestId == request) {
                result = response
            }
        } catch (e: CancellationException) {
            throw e
        } finally {
            if (requestId == request) {
                isChecking = false
            }
        }
    }

    LaunchedEffect(password, autoCheck) {
        requestId++
        val request = requestId
        result = null
        isChecking = false

        if (autoCheck && password.isNotEmpty()) {
            performCheck(password, request)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!autoCheck) {
            OutlinedButton(
                onClick = {
                    if (password.isNotEmpty() && !isChecking) {
                        requestId++
                        val request = requestId

                        scope.launch {
                            performCheck(password, request)
                        }
                    }
                },
                enabled = password.isNotEmpty() && !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Checking password…")
                    }
                } else {
                    Text("Check for breach")
                }
            }
        } else if (isChecking) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    "Checking password security…",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        when (val currentResult = result) {
            is PasswordBreachResult.Found -> {

                val occurrences = currentResult.occurrences

                val uriHandler = LocalUriHandler.current
                val websiteUrl = getSafeWebsiteUrl(website)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security warning",
                                tint = MaterialTheme.colorScheme.error
                            )

                            Text(
                                text = "Password exposed in known breaches",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Text(
                            text = "Found in ${
                                "%,d".format(java.util.Locale.US, occurrences)
                            } breach records. Change this password as soon as possible. " +
                                    "Don't reuse it on other websites.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (websiteUrl != null) {
                            OutlinedButton(
                                onClick = { uriHandler.openUri(websiteUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Open website to change password")
                            }
                        } else {
                            Text(
                                text = "No valid website is saved. Open the service's " +
                                        "official website manually.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }


            PasswordBreachResult.NotFound -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Breach check information",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No breach match found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "This password wasn't found in the known " +
                                        "breach database. This doesn't guarantee it is safe.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }



            PasswordBreachResult.NetworkError -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Network error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Couldn't check password",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "The breach check couldn't be completed. " +
                                            "Your password's breach status is unknown.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isChecking && password.isNotEmpty()) {
                                    requestId++
                                    val request = requestId

                                    scope.launch {
                                        performCheck(password, request)
                                    }
                                }
                            },
                            enabled = !isChecking && password.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry check")
                        }
                    }
                }
            }
            null -> Unit
        }
    }
}

private fun getSafeWebsiteUrl(website: String?): String? {
    val input = website?.trim()
        ?.takeIf { it.isNotEmpty() && !it.any(Char::isWhitespace) }
        ?: return null

    val candidate = if (
        input.startsWith("https://", ignoreCase = true) ||
        input.startsWith("http://", ignoreCase = true)
    ) {
        input
    } else {
        "https://$input"
    }

    return try {
        val uri = Uri.parse(candidate)

        candidate.takeIf {
            uri.scheme?.lowercase() in setOf("https", "http") &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null
        }
    } catch (_: IllegalArgumentException) {
        null
    }
}
