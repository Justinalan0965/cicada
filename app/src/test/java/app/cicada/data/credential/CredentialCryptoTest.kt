package app.cicada.data.credential

import app.cicada.security.CryptoManager
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CredentialCryptoTest {

    private val cryptoManager = CryptoManager()
    private val json = Json

    @Test
    fun `credential can be serialized, encrypted, decrypted and deserialized`() {

        val original = Credential(
            id = "123",
            title = "Dummy Circle",
            username = "dummy",
            password = "password",
            website = "www.dummy.com",
            notes = "Dummy Account"
        )

        val vaultKey = cryptoManager.generateVaultKey()

        try {
            val serialized = json.encodeToString(original)

            val plainText = serialized.toByteArray(Charsets.UTF_8)
            try {

                val encryptedData = cryptoManager.encrypt(plainText, vaultKey)

                assertNotEquals(
                    serialized,
                    encryptedData.toString(Charsets.UTF_8)
                )

                val decryptedData = cryptoManager.decrypt(encryptedData, vaultKey)

                try {
                    val decryptedJson = decryptedData.toString(Charsets.UTF_8)

                    val restored = json.decodeFromString<Credential>(decryptedJson)

                    assertEquals(
                        restored,
                        original
                    )

                    assertArrayEquals(
                        decryptedData,
                        plainText
                    )
                } finally {
                    decryptedData.fill(0)
                }
            } finally {
                plainText.fill(0)
            }
        } finally {
            vaultKey.fill(0)
        }
    }
}