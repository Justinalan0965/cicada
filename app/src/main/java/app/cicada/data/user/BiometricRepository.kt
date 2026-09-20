package app.cicada.data.user

import app.cicada.security.BiometricKeyManager
import app.cicada.security.VaultSession
import javax.crypto.Cipher

class BiometricRepository(
    private val userDAO: UserDAO,
    private val vaultSession: VaultSession
) {

    fun prepareEnrollmentCipher(): Cipher {
        val userId = vaultSession.getUserId()

        val keyManager = BiometricKeyManager(userId)

        keyManager.createKey()

        return keyManager.createEncryptCipher()
    }

    suspend fun saveBiometricVaultKey(
        cipher: Cipher
    ) {
        val userId = vaultSession.getUserId()
        val vaultKey = vaultSession.getKey()

        val keyManager = BiometricKeyManager(userId)

        try {
            val encryptedVaultKey =
                keyManager.encrypt(
                    cipher,
                    vaultKey
                )

            userDAO.updateBiometricKey(
                userId = userId,
                encryptedBiometricKey = encryptedVaultKey
            )
        } finally {
            vaultKey.fill(0)
        }
    }

    suspend fun prepareBiometricUnlock(
        username: String
    ): BiometricUnlockData? {

        val user = userDAO.getUserByUsername(
            username.trim().lowercase()
        ) ?: return null

        val encryptedVaultKey =
            user.encryptedBiometricKey
                ?: return null

        return BiometricUnlockData(
            userId = user.id,
            encryptedVaultKey = encryptedVaultKey
        )
    }

    fun prepareDecryptionCipher(
        userId: String,
        encryptedVaultKey: ByteArray
    ): Cipher {

        val keyManager = BiometricKeyManager(userId)

        return keyManager.createDecryptCipher(
            encryptedVaultKey
        )
    }

    fun decryptVaultKey(
        userId: String,
        encryptedVaultKey: ByteArray,
        authenticatedCipher: Cipher
    ): ByteArray {

        val keyManager = BiometricKeyManager(userId)

        return keyManager.decrypt(
            authenticatedCipher,
            encryptedVaultKey
        )
    }

    suspend fun isBiometricEnabled(username: String): Boolean {
        val user = userDAO.getUserByUsername(
            username.trim().lowercase()
        )

        return user?.encryptedBiometricKey != null
    }

    suspend fun isBiometricEnabledForCurrentUser(): Boolean {
        val userId = vaultSession.getUserId()

        val user = userDAO.getUserById(userId)
            ?: return false

        return user.encryptedBiometricKey != null &&
                BiometricKeyManager(userId).hasKey()
    }

    suspend fun disableBiometric() {
        val userId = vaultSession.getUserId()

        userDAO.updateBiometricKey(
            userId = userId,
            encryptedBiometricKey = null
        )

        BiometricKeyManager(userId).deleteKey()
    }

    fun cancelEnrollment() {
        val userId = vaultSession.getUserId()
        BiometricKeyManager(userId).deleteKey()
    }
}

data class BiometricUnlockData(
    val userId: String,
    val encryptedVaultKey: ByteArray
)