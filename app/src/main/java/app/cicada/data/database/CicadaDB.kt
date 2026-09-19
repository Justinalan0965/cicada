package app.cicada.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cicada.data.credential.CredentialDAO
import app.cicada.data.credential.CredentialEntity
import app.cicada.data.user.UserDAO
import app.cicada.data.user.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CredentialEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class CicadaDB : RoomDatabase() {

    abstract fun userDAO(): UserDAO

    abstract fun credentialDAO(): CredentialDAO

    companion object {

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    ALTER TABLE credentials
                    RENAME COLUMN vaultId TO userId
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: CicadaDB? = null

        fun getInstance(context: Context): CicadaDB {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CicadaDB::class.java,
                    "cicada_db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}