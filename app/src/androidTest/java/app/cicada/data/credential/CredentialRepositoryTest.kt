package app.cicada.data.credential

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cicada.data.database.CicadaDB
import app.cicada.security.CryptoManager
import app.cicada.security.VaultSession
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialRepositoryTest {

    private lateinit var database: CicadaDB
    private lateinit var repository: CredentialRepository

    private lateinit var cryptoManager: CryptoManager
    private lateinit var vaultSession: VaultSession

    @Before
    fun setup() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            CicadaDB::class.java
        )
            .allowMainThreadQueries()
            .build()

        cryptoManager = CryptoManager()
        vaultSession = VaultSession()

        val vaultKey = cryptoManager.generateVaultKey()

        vaultSession.unlock(
            vaultId = "test-vault",
            key = vaultKey
        )

        vaultKey.fill(0)

        repository = CredentialRepository(
            credentialDAO = database.credentialDAO(),
            cryptoManager = cryptoManager,
            vaultSession = vaultSession
        )
    }

    @After
    fun tearDown() {
        vaultSession.lock()
        database.close()
    }

    @Test
    fun credential_can_be_saved_and_loaded() = runBlocking {

        repository.addCredentialToVault(
            title = "GitHub",
            username = "dummy",
            password = "password123",
            website = "www.github.com",
            notes = "Test account"
        )

        val credentials = repository
            .getCredentials()
            .first()

        assertEquals(1, credentials.size)

        val credential = credentials.first()

        assertEquals("GitHub", credential.title)
        assertEquals("dummy", credential.username)
        assertEquals("password123", credential.password)
        assertEquals("www.github.com", credential.website)
        assertEquals("Test account", credential.notes)
    }
}