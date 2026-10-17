    package app.cicada.data.credential

    import android.net.Uri
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

            val userId = vaultSession.getUserId()
            val vaultKey =  vaultSession.getKey()

            try {
                val serialized = json.encodeToString(credential)

                val plaintext = serialized.toByteArray(Charsets.UTF_8)

                try {
                    val encryptedData = cryptoManager.encrypt(
                        plaintext,
                        vaultKey
                    )

                    val websiteDomain = credential.website
                        ?.let { extractDomain(it) }


                    val entity = CredentialEntity(
                        id = credential.id,
                        userId = userId,
                        websiteDomain = websiteDomain,
                        username = credential.username,
                        encryptedData = encryptedData,
                        createdTime = System.currentTimeMillis()
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
            val userId = vaultSession.getUserId()

            return credentialDAO
                .getAllCredentials(userId)
                .map { entities ->

                    val vaultKey = vaultSession.getKey()

                    try {
                        entities.map { entity ->
                            getAsCredential(entity, vaultKey)
                        }
                    } finally {
                        vaultKey.fill(0)
                    }
                }
        }

        suspend fun getCredential(credentialId: String): Credential {
            val userId = vaultSession.getUserId()
            val vaultKey = vaultSession.getKey()

            try {
                val entity = credentialDAO.getCredentialById(userId, credentialId)

                return getAsCredential(entity, vaultKey)
            } finally {
                vaultKey.fill(0)
            }    }

        private fun getAsCredential(entity: CredentialEntity, vaultKey: ByteArray): Credential {
            val decryptedData = cryptoManager.decrypt(
                entity.encryptedData,
                vaultKey
            )

            return try {
                val serialized = decryptedData.toString(Charsets.UTF_8)

                json.decodeFromString<Credential>(serialized)
            } finally {
                decryptedData.fill(0)
            }
        }

        suspend fun updateCredential(
            credentialId: String,
            title: String,
            username: String,
            password: String,
            website: String,
            notes: String
        ) {

            val userId = vaultSession.getUserId()
            val vaultKey = vaultSession.getKey()

            try {
                val oldEntity = credentialDAO.getCredentialById(userId, credentialId)

                val credential = Credential(
                    id = credentialId,
                    title = title,
                    username = username,
                    password = password,
                    website = website,
                    notes = notes
                )

                val serialized = json.encodeToString(credential)
                val plainText = serialized.toByteArray(Charsets.UTF_8)

                try {
                    val encryptedData = cryptoManager.encrypt(plainText, vaultKey)
                    val websiteDomain = credential.website
                        ?.let { extractDomain(it) }

                    val updateEntity = CredentialEntity(
                        id = oldEntity.id,
                        userId = oldEntity.userId,
                        websiteDomain = websiteDomain,
                        username = credential.username,
                        encryptedData = encryptedData,
                        createdTime = oldEntity.createdTime
                    )

                    credentialDAO.updateCredential(updateEntity)
                } finally {
                    plainText.fill(0)
                }
            } finally {
                vaultKey.fill(0)
            }
        }

        suspend fun deleteCredential(credentialId: String) {
            val userId = vaultSession.getUserId()
            val credentialEntity = credentialDAO.getCredentialById(userId, credentialId)

            credentialDAO.deleteCredential(credentialEntity)
        }

        private fun extractDomain(website: String): String? {
            return try {
                val normalized = if (
                    website.startsWith("http://") ||
                    website.startsWith("https://")
                ) {
                    website
                } else {
                    "https://$website"
                }

                Uri
                    .parse(normalized)
                    .host
                    ?.lowercase()
                    ?.removePrefix("www.")
            } catch (e: Exception) {
                null
            }
        }
    }