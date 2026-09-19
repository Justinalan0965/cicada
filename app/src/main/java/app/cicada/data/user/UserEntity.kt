package app.cicada.data.user

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity (
    @PrimaryKey
    val id: String,

    val username: String,

    val salt: ByteArray,

    val encryptedKey: ByteArray
)
