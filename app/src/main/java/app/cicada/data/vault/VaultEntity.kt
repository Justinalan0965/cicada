package app.cicada.data.vault

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaults")
data class VaultEntity (
    @PrimaryKey
    val id: String,

    val name: String,

    val salt: ByteArray,

    val encryptedKey: ByteArray
)
