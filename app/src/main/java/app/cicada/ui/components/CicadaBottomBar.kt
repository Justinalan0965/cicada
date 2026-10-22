package app.cicada.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

enum class Destination(
    val route: String,
    val label: String,
    val contentDescription: String,
    val iconVector: ImageVector? = null,
    @DrawableRes val iconDrawable: Int? = null
) {
    Vault(
        route = "home",
        label = "Home",
        iconVector = Icons.Default.Home,
        contentDescription = "Home page"
    ),
    PasswordGenerator(
        route = "pwdGenerator",
        label = "Generator",
        contentDescription = "Password Generator",
        iconVector = Icons.Default.Password
    ),
    Settings(
        route = "settings",
        label = "Settings",
        iconVector = Icons.Default.Settings,
        contentDescription = "App Settings"
    )
}

@Composable
fun CicadaBottomBar(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (destination.iconVector != null) {
                        Icon(
                            imageVector = destination.iconVector,
                            contentDescription = destination.contentDescription
                        )
                    } else if (destination.iconDrawable != null) {
                        Icon(
                            painter = painterResource(id = destination.iconDrawable),
                            contentDescription = destination.contentDescription,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}