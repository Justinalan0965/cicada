package app.cicada.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.cicada.data.credential.CredentialRepository
import app.cicada.data.database.CicadaDB
import app.cicada.data.vault.VaultRepository
import app.cicada.security.CicadaClipboardManager
import app.cicada.security.CryptoManager
import app.cicada.security.VaultSession
import app.cicada.ui.addCredentialPage.AddCredentialScreen
import app.cicada.ui.createVaultPage.CreateVaultScreen
import app.cicada.ui.homePage.HomeScreen
import app.cicada.ui.loginPage.LoginScreen
import app.cicada.ui.settingsPage.SettingScreen
import app.cicada.ui.viewCredentialPage.ViewCredentialScreen
import app.cicada.viewmodel.addCredential.AddCredentialViewModel
import app.cicada.viewmodel.addCredential.AddCredentialViewModelFactory
import app.cicada.viewmodel.createVault.CreateVaultViewModel
import app.cicada.viewmodel.createVault.CreateVaultViewModelFactory
import app.cicada.viewmodel.home.HomeViewModel
import app.cicada.viewmodel.home.HomeViewModelFactory
import app.cicada.viewmodel.login.LoginViewModel
import app.cicada.viewmodel.login.LoginViewModelFactory
import app.cicada.viewmodel.viewCredential.ViewCredentialViewModel
import app.cicada.viewmodel.viewCredential.ViewCredentialViewModelFactory

@Composable
fun CicadaNavigation() {

    val navController = rememberNavController()

    val context = LocalContext.current

    val clipboardManager = remember {
        CicadaClipboardManager(context)
    }

    val database = remember {
        CicadaDB.getInstance(context)
    }

    val cryptoManager = remember {
        CryptoManager()
    }

    val vaultRepository = remember {
        VaultRepository(
            database.vaultDAO(),
            cryptoManager = cryptoManager
        )
    }

    val vaultSession = remember {
        VaultSession()
    }

    val credentialRepository = remember {
        CredentialRepository(
            credentialDAO = database.credentialDAO(),
            cryptoManager = cryptoManager,
            vaultSession = vaultSession
        )
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("createVault") {
            val createVaultViewModel : CreateVaultViewModel = viewModel(
                factory = CreateVaultViewModelFactory(vaultRepository)
            )

            CreateVaultScreen(
                onVaultCreated = {
                    navController.navigate("login") {
                        popUpTo("createVault") {
                            inclusive = true
                        }
                    }
                },
                createVaultViewModel = createVaultViewModel
            )
        }

        composable("login") {

            val loginViewModel: LoginViewModel =
                viewModel(
                    factory = LoginViewModelFactory(
                        vaultRepository = vaultRepository,
                        vaultSession = vaultSession
                    )
                )

            LoginScreen (
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") {
                            inclusive = true
                        }
                    }
                },

                onCreateVault = {
                    navController.navigate("createVault")
                },
                loginViewModel = loginViewModel
            )
        }

        composable("home") {
            val homeViewModel : HomeViewModel = viewModel(
                factory = HomeViewModelFactory(
                    credentialRepository = credentialRepository
                )
            )
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            HomeScreen(
                homeViewModel = homeViewModel,
                onNavigateToAdd = {
                    navController.navigate("addCredential")
                },
                currentRoute = currentRoute,
                onNavigateBottomBar = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onClick = { credentialId -> navController.navigate("viewCredential/${credentialId}") }
            )
        }

        composable("addCredential") {
            val addCredentialViewModel : AddCredentialViewModel = viewModel(
                factory = AddCredentialViewModelFactory(
                    credentialRepository = credentialRepository
                )
            )

            AddCredentialScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                addCredentialViewModel = addCredentialViewModel
            )
        }

        composable(
            route = "viewCredential/{credentialId}",
            enterTransition = {
                scaleIn(initialScale = 0.9f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                scaleOut(targetScale = 0.9f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            }
        )  { backStackEntry ->
            val credentialId = backStackEntry.arguments?.getString("credentialId")

            val viewCredentialViewModel: ViewCredentialViewModel = viewModel(
                factory = ViewCredentialViewModelFactory(
                    credentialRepository = credentialRepository
                )
            )

            ViewCredentialScreen(
                viewCredentialViewModel = viewCredentialViewModel,
                credentialId = credentialId ?: "",
                clipboardManager = clipboardManager,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEdit = {
                    navController.navigate("editCredential/$credentialId")
                }
            )
        }

        composable(
            route = "editCredential/{credentialId}"
        ) { backStackEntry ->

            val credentialId = backStackEntry.arguments?.getString("credentialId")

            val addCredentialViewModel: AddCredentialViewModel = viewModel(
                factory = AddCredentialViewModelFactory(
                    credentialRepository = credentialRepository
                )
            )

            AddCredentialScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                addCredentialViewModel = addCredentialViewModel,
                credentialId = credentialId
            )
        }

        composable(route = "settings") {
            SettingScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}