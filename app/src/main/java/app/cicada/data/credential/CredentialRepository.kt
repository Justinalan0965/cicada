package app.cicada.data.credential

import app.cicada.security.CryptoManager
import app.cicada.security.VaultSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

class CredentialRepository(
    private val credentialDAO: CredentialDAO,
    private val cryptoManager: CryptoManager,
    private val vaultSession: VaultSession
) {

    private val json = Json

    suspend fun addCredentialToVault(title: String, username: String, password: String, website: String, notes: String) {
        val credential = Credential(
            id = UUID.randomUUID().toString(),
            title = title,
            username = username,
            password = password,
            website = website,
            notes = notes
        )

        val vaultId = vaultSession.getVaultId()
        val vaultKey =  vaultSession.getKey()

        try {
            val serialized = json.encodeToString(credential)

            val plaintext = serialized.toByteArray(Charsets.UTF_8)

            try {
                val encryptedData = cryptoManager.encrypt(
                    plaintext,
                    vaultKey
                )

                val entity = CredentialEntity(
                    credential.id,
                    vaultId,
                    encryptedData,
                    System.currentTimeMillis()
                )

                credentialDAO.addCredential(entity)
            } finally {
                plaintext.fill(0)
            }
        } finally {
            vaultKey.fill(0)
        }
    }

    fun getCredentials() : Flow<List<Credential>> {
        val vaultId = vaultSession.getVaultId()

        return credentialDAO
            .getAllCredentials(vaultId)
            .map { entities ->

                val vaultKey = vaultSession.getKey()

                try {
                    entities.map { entity ->

                        val decryptedData = cryptoManager.decrypt(
                            entity.encryptedData,
                            vaultKey
                        )

                        try {
                            val serialized = decryptedData.toString(Charsets.UTF_8)

                            json.decodeFromString<Credential>(serialized)
                        } finally {
                            decryptedData.fill(0)
                        }
                    }
                } finally {
                    vaultKey.fill(0)
                }
            }
    }
}