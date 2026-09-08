package app.cicada.data.vault

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDAO {

    @Query("SELECT * FROM vaults ORDER BY name")
    fun getAllVaults(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vaults WHERE id = :vaultId LIMIT 1")
    suspend fun getVaultByID(vaultId: String): VaultEntity?

    @Insert
    suspend fun insertVault(vault: VaultEntity)

    @Delete
    suspend fun deleteVault(vault: VaultEntity)
}