package app.cicada.data.credential

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credentials")
class CredentialEntity (

    @PrimaryKey
    val id: String,

    val userId: String,

    val encryptedData: ByteArray,

    val createdTime: Long
)