package app.cicada.data.credential

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDAO {

    @Query("SELECT * FROM credentials WHERE vaultId = :vaultId ORDER BY createdTime")
    fun getAllCredentials(vaultId: String): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE vaultId = :vaultId and id = :credentialId LIMIT 1")
    suspend fun getCredentialById(vaultId: String, credentialId: String): CredentialEntity

    @Insert
    suspend fun addCredential(credential: CredentialEntity)

    @Update
    suspend fun updateCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)
}