package app.cicada.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.jcajce.provider.symmetric.ARGON2
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {

    companion object {
        private const val SALT_SIZE = 32
        private const val KEY_SIZE = 32
        private const val NONCE_SIZE = 12
        private const val TAG_SIZE = 128

        private const val ARGON2_MEM_KIB = 64 * 1024
        private const val ARGON2_ITERATION = 3
        private const val ARGON2_PARALLELISM = 1
    }

    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        return ByteArray(SALT_SIZE).also{
            secureRandom.nextBytes(it)
        }
    }

    fun generateVaultKey(): ByteArray {
        return ByteArray(KEY_SIZE).also {
            secureRandom.nextBytes(it)
        }
    }

    fun deriveMasterKey(password: CharArray, salt: ByteArray): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withIterations(ARGON2_ITERATION)
            .withParallelism(ARGON2_PARALLELISM)
            .withMemoryAsKB(ARGON2_MEM_KIB)
            .build()

        val generator = Argon2BytesGenerator()

        generator.init(parameters)

        val passwordAsBytes = String(password).toByteArray(Charsets.UTF_8)

        val deriveKey = ByteArray(KEY_SIZE)

        generator.generateBytes(passwordAsBytes, deriveKey)

        passwordAsBytes.fill(0)

        return deriveKey
    }

    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) {
            "AES-256 requires a 32-byte key"
        }

        val nonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val secretKey = SecretKeySpec(key, "AES")

        val gcmSpec = GCMParameterSpec(TAG_SIZE, nonce)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)

        return nonce + ciphertext
    }

    fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray {
        require(key.size == KEY_SIZE) {
            "AES-25 requires a 32-byte key"
        }

        require(encryptedData.size > NONCE_SIZE) {
            "Invalid encrypted data"
        }

        val nonce = encryptedData.copyOfRange(0, NONCE_SIZE)

        val cipherText = encryptedData.copyOfRange(NONCE_SIZE, encryptedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val secretKey = SecretKeySpec(key, "AES")

        val gcmSpec = GCMParameterSpec(TAG_SIZE, nonce)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(cipherText)

    }
}