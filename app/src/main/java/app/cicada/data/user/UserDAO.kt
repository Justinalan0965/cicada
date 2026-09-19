package app.cicada.data.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDAO {

    @Query("SELECT * FROM users ORDER BY username")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    suspend fun usernameExists(username: String): Boolean

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert
    suspend fun addUser(vault: UserEntity)

    @Delete
    suspend fun deleteUser(vault: UserEntity)
}