package app.cicada.data.credential

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDAO {

    @Query("SELECT * FROM credentials WHERE vaultId = :vaultId ORDER BY createdTime")
    fun getAllCredentials(vaultId: String): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials WHERE id = :id LIMIT 1")
    fun getCredentialById(id: String): CredentialEntity

    @Insert
    suspend fun addCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)
}