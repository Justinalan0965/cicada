package app.cicada.data.vault

import app.cicada.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID


class VaultRepository(
    private val vaultDao: VaultDAO,
    private val cryptoManager: CryptoManager
) {

    private val vaults = emptyList<VaultInfo>()

    fun getVaults(): Flow<List<VaultInfo>> {
        return vaultDao
            .getAllVaults()
            .map { entities ->
                entities.map{ entity ->
                    VaultInfo(entity.id, entity.name)
                }
            }
    }

    suspend fun createVault(name: String, password: CharArray) {

        val id = UUID.randomUUID().toString()

        val salt = cryptoManager.generateSalt()

        val masterKey = cryptoManager.deriveMasterKey(password, salt)

        try {
            val vaultKey = cryptoManager.generateVaultKey()

            val  encryptedVaultKey = cryptoManager.encrypt(vaultKey, masterKey)

            val vault = VaultEntity(
                id,
                name,
                salt,
                encryptedVaultKey
            )

            vaultDao.insertVault(vault)

        } finally {
            masterKey.fill(0)
        }
    }

    suspend fun deleteVault(vaultId: String) {
        val vault = vaultDao.getVaultByID(vaultId)

        if (vault != null) {
            vaultDao.deleteVault(vault)
        }
    }

    suspend fun unlockVault(vaultId: String, password: CharArray): ByteArray? = withContext(Dispatchers.IO){
        val vault = vaultDao.getVaultByID(vaultId) ?: return@withContext null

        val masterKey = cryptoManager.deriveMasterKey(password, vault.salt)

        try {
            cryptoManager.decrypt(vault.encryptedKey, masterKey)
        } catch (e : Exception) {
            null
        } finally {
            masterKey.fill(0.toByte())
        }
    }
}