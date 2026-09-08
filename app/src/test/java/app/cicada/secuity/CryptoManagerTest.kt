package app.cicada.secuity

import app.cicada.security.CryptoManager
import org.junit.Test
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows

class CryptoManagerTest {

    private val cryptoManager = CryptoManager()

    @Test
    fun `encrypted data can be decrypted`() {
        val password = "MyVeryVerySecretPassword".toCharArray()


        val salt = cryptoManager.generateSalt()

        val masterKey =  cryptoManager.deriveMasterKey(password, salt)

        val originalData = "Cicada secret data".toByteArray()

        val encryptedData = cryptoManager.encrypt(originalData, masterKey)

        val decryptedData = cryptoManager.decrypt(encryptedData, masterKey)


        println("Encrypted Data : $encryptedData | Decrypted Data : $decryptedData")


        assertArrayEquals(
            originalData,
            decryptedData
        )
    }

    @Test
    fun `different salts produce different keys`() {
        val password = "MyVeryVeryStrongPassword".toCharArray()

        val salt1 = cryptoManager.generateSalt()
        val salt2 = cryptoManager.generateSalt()

        val key1 = cryptoManager.deriveMasterKey(password, salt1)
        val key2 = cryptoManager.deriveMasterKey(password, salt2)

        assert(
            !key1.contentEquals(key2)
        )
    }

    @Test
    fun `same password and salt produce same key`() {
        val password = "MyVeryStrongPassword123!".toCharArray()

        val salt = cryptoManager.generateSalt()

        val key1 = cryptoManager.deriveMasterKey(
            password = password,
            salt = salt
        )

        val key2 = cryptoManager.deriveMasterKey(
            password = password,
            salt = salt
        )

        assertArrayEquals(
            key1,
            key2
        )
    }

    @Test
    fun `wrong key cannot decrypt data`() {

        val password1 = "MyVeryStrongPassword123!".toCharArray()
        val password2 = "CompletelyDifferentPassword!".toCharArray()

        val salt = cryptoManager.generateSalt()

        val key1 = cryptoManager.deriveMasterKey(
            password = password1,
            salt = salt
        )

        val key2 = cryptoManager.deriveMasterKey(
            password = password2,
            salt = salt
        )

        val originalData =
            "Secret Cicada data".toByteArray()

        val encryptedData = cryptoManager.encrypt(
            plaintext = originalData,
            key = key1
        )

        assertThrows(Exception::class.java) {

            cryptoManager.decrypt(
                encryptedData = encryptedData,
                key = key2
            )
        }
    }

    @Test
    fun `each encryption gets a different nonce`() {

        val key = cryptoManager.generateVaultKey()

        val data =
            "Same plaintext".toByteArray()

        val encrypted1 = cryptoManager.encrypt(
            plaintext = data,
            key = key
        )

        val encrypted2 = cryptoManager.encrypt(
            plaintext = data,
            key = key
        )

        assert(
            !encrypted1.contentEquals(encrypted2)
        )
    }
}