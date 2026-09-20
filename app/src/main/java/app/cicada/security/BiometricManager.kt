package app.cicada.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricKeyManager(
    private val userId: String
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS_PREFIX = "cicada_biometric_"

        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128
    }

    private val keyAlias =
        KEY_ALIAS_PREFIX + userId

    private val keyStore: KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }

    fun createKey() {
        if (hasKey()) {
            return
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keySpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or
                KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
            .build()

        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    fun hasKey(): Boolean {
        return keyStore.containsAlias(keyAlias)
    }

    private fun getKey(): SecretKey {
        return keyStore.getKey(
            keyAlias,
            null
        ) as SecretKey
    }

    fun createEncryptCipher(): Cipher {
        return Cipher.getInstance(
            "AES/GCM/NoPadding"
        ).apply {
            init(
                Cipher.ENCRYPT_MODE,
                getKey()
            )
        }
    }

    fun createDecryptCipher(
        encryptedData: ByteArray
    ): Cipher {
        require(encryptedData.size > GCM_IV_SIZE) {
            "Invalid encrypted data"
        }

        val iv = encryptedData.copyOfRange(
            0,
            GCM_IV_SIZE
        )

        return Cipher.getInstance(
            "AES/GCM/NoPadding"
        ).apply {
            init(
                Cipher.DECRYPT_MODE,
                getKey(),
                GCMParameterSpec(
                    GCM_TAG_SIZE,
                    iv
                )
            )
        }
    }

    fun encrypt(
        cipher: Cipher,
        plaintext: ByteArray
    ): ByteArray {
        val ciphertext = cipher.doFinal(plaintext)

        return cipher.iv + ciphertext
    }

    fun decrypt(
        cipher: Cipher,
        encryptedData: ByteArray
    ): ByteArray {
        require(encryptedData.size > GCM_IV_SIZE) {
            "Invalid encrypted data"
        }

        val ciphertext = encryptedData.copyOfRange(
            GCM_IV_SIZE,
            encryptedData.size
        )

        return cipher.doFinal(ciphertext)
    }

    fun deleteKey() {
        if (hasKey()) {
            keyStore.deleteEntry(keyAlias)
        }
    }
}