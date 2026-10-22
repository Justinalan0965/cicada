package app.cicada.ui.pwdGeneratorPage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cicada.security.CicadaClipboardManager
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(
    clipboardManager: CicadaClipboardManager
) {
    var password by remember { mutableStateOf("") }
    var length by remember { mutableFloatStateOf(16f) }

    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var regenerateToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(
        length,
        includeUppercase,
        includeLowercase,
        includeNumbers,
        includeSymbols,
        regenerateToken
    ) {
        password = generateSecurePassword(
            length = length.toInt(),
            includeUppercase = includeUppercase,
            includeLowercase = includeLowercase,
            includeNumbers = includeNumbers,
            includeSymbols = includeSymbols
        )
    }


    val onToggle: (Boolean, (Boolean) -> Unit) -> Unit = { newValue, updateState ->
        val activeCount = listOf(
            includeUppercase,
            includeLowercase,
            includeNumbers,
            includeSymbols
        ).count { it }

        if (newValue || activeCount > 1) {
            updateState(newValue)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generator", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Password Display Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = password,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                regenerateToken++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Regenerate")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { clipboardManager.copy("Password", password) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 2. Length Slider Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Length",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${length.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = length,
                    onValueChange = {
                        length = it
                    },
                    valueRange = 8f..64f,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Configuration Toggles Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column {
                    GeneratorToggleRow(
                        title = "Uppercase (A-Z)",
                        isChecked = includeUppercase,
                        onCheckedChange = { onToggle(it) { v -> includeUppercase = v } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    GeneratorToggleRow(
                        title = "Lowercase (a-z)",
                        isChecked = includeLowercase,
                        onCheckedChange = { onToggle(it) { v -> includeLowercase = v } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    GeneratorToggleRow(
                        title = "Numbers (0-9)",
                        isChecked = includeNumbers,
                        onCheckedChange = { onToggle(it) { v -> includeNumbers = v } }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    GeneratorToggleRow(
                        title = "Symbols (!@#$)",
                        isChecked = includeSymbols,
                        onCheckedChange = { onToggle(it) { v -> includeSymbols = v } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun GeneratorToggleRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}


private val passwordSecureRandom = SecureRandom()

private fun generateSecurePassword(
    length: Int,
    includeUppercase: Boolean,
    includeLowercase: Boolean,
    includeNumbers: Boolean,
    includeSymbols: Boolean
): String {
    val selectedPools = buildList<List<Char>> {
        if (includeUppercase) add(('A'..'Z').toList())
        if (includeLowercase) add(('a'..'z').toList())
        if (includeNumbers) add(('0'..'9').toList())
        if (includeSymbols) {
            add(listOf('!', '@', '#', '$', '%', '^', '&', '*', '?', '_', '-'))
        }
    }

    require(selectedPools.isNotEmpty()) {
        "Select at least one character category."
    }
    require(length >= selectedPools.size) {
        "Password length is too short for the selected categories."
    }

    val combinedPool = selectedPools.flatten()
    val characters = ArrayList<Char>(length)

    selectedPools.forEach { pool ->
        characters.add(pool[passwordSecureRandom.nextInt(pool.size)])
    }

    while (characters.size < length) {
        characters.add(
            combinedPool[passwordSecureRandom.nextInt(combinedPool.size)]
        )
    }


    for (i in characters.lastIndex downTo 1) {
        val j = passwordSecureRandom.nextInt(i + 1)
        val temp = characters[i]
        characters[i] = characters[j]
        characters[j] = temp
    }

    return characters.joinToString("")
}
