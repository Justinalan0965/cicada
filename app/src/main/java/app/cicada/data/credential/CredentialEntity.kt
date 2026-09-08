package app.cicada.data.credential

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "credentials")
class CredentialEntity (

    @PrimaryKey
    val id: String,

    val vaultId: String,

    val encryptedData: ByteArray,

    val createdTime: Long
)