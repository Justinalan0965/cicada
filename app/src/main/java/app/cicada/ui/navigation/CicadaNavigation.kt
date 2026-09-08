package app.cicada.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.cicada.data.credential.CredentialRepository
import app.cicada.data.database.CicadaDB
import app.cicada.data.vault.VaultRepository
import app.cicada.security.CryptoManager
import app.cicada.security.VaultSession
import app.cicada.ui.addCredential.AddCredentialScreen
import app.cicada.ui.createVaultPage.CreateVaultScreen
import app.cicada.ui.homePage.HomeScreen
import app.cicada.ui.loginPage.LoginScreen
import app.cicada.viewmodel.addCredential.AddCredentialViewModel
import app.cicada.viewmodel.addCredential.AddCredentialViewModelFactory
import app.cicada.viewmodel.createVault.CreateVaultViewModel
import app.cicada.viewmodel.createVault.CreateVaultViewModelFactory
import app.cicada.viewmodel.home.HomeViewModel
import app.cicada.viewmodel.home.HomeViewModelFactory
import app.cicada.viewmodel.login.LoginViewModel
import app.cicada.viewmodel.login.LoginViewModelFactory

@Composable
fun CicadaNavigation() {

    val navController = rememberNavController()

    val context = LocalContext.current

    val database = remember {
        CicadaDB.getInstance(context)
    }

    val vaultRepository = remember {
        VaultRepository(
            database.vaultDAO(),
            cryptoManager = CryptoManager()
        )
    }



    val cryptoManager = remember {
        CryptoManager()
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

            HomeScreen(
                homeViewModel = homeViewModel,
                onNavigateToAdd = {
                    navController.navigate("addCredential")
                }
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
    }
}