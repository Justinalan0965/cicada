package app.cicada.data.user

import app.cicada.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID


class UserRepository(
    private val userDAO: UserDAO,
    private val cryptoManager: CryptoManager
) {

    suspend fun usernameExists(username : String) : Boolean {
        val normalizedUsername = normalizeUsername(username)
        return userDAO.usernameExists(normalizedUsername)
    }

    suspend fun createUser(username: String, password: CharArray) {

        val normalizedUsername = normalizeUsername(username)

        val userId = UUID.randomUUID().toString()
        val salt = cryptoManager.generateSalt()

        val masterKey = cryptoManager.deriveMasterKey(password, salt)

        try {
            val userKey = cryptoManager.generateVaultKey()

            try {

                val encryptedUserKey = cryptoManager.encrypt(userKey, masterKey)

                val user = UserEntity(
                    userId,
                    normalizedUsername,
                    salt,
                    encryptedUserKey
                )

                userDAO.addUser(user)

            } finally {
                userKey.fill(0)
            }
        } finally {
            masterKey.fill(0)
        }
    }

    suspend fun deleteUser(userId: String) {
        val user = userDAO.getUserById(userId)

        if (user != null) {
            userDAO.deleteUser(user)
        }
    }

    suspend fun unlockUser(username: String, password: CharArray): UnlockResult? = withContext(Dispatchers.IO){
        val normalizedUsername = normalizeUsername(username)

        val user = userDAO.getUserByUsername(normalizedUsername) ?: return@withContext null

        val masterKey = cryptoManager.deriveMasterKey(password, user.salt)
        try {
            val vaultKey = try {
                 cryptoManager.decrypt(user.encryptedKey, masterKey)
            } catch (e: Exception) {
                return@withContext null
            }

            UnlockResult(
                userId = user.id,
                vaultKey = vaultKey
            )
        }finally {
            masterKey.fill(0)
        }
    }
    fun normalizeUsername(username : String) : String {
        return username.trim().lowercase()
    }
}