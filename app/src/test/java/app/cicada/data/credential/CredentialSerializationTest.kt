package app.cicada.data.credential

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class CredentialSerializationTest {

    private val json = Json

    @Test
    fun `credential can be serialized and deserialized`() {
        val originalData = Credential(
            id = "123",
            title = "Dummy Circle",
            website = "www.dummy.com",
            username = "dummy",
            password = "password",
            notes = "Dummy Account"
        )

        val serialized = json.encodeToString(originalData)
        println("serialized :: $serialized")

        val restored = json.decodeFromString<Credential>(serialized)
        println("deserialized :: $restored")

        assertEquals(restored, originalData)
    }
}