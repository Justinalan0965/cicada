package app.cicada.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.cicada.data.credential.CredentialDAO
import app.cicada.data.credential.CredentialEntity
import app.cicada.data.vault.VaultDAO
import app.cicada.data.vault.VaultEntity

@Database(
    entities = [
        VaultEntity::class,
        CredentialEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CicadaDB: RoomDatabase() {

    abstract fun vaultDAO(): VaultDAO

    abstract fun credentialDAO(): CredentialDAO

    companion object {

        @Volatile
        private var INSTANCE: CicadaDB? = null

        fun getInstance(context: Context): CicadaDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CicadaDB::class.java,
                    "cicada_db"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }

}