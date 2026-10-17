
package app.cicada.autofill

import android.content.Context
import android.service.autofill.SaveCallback
import android.util.Log
import app.cicada.data.credential.CredentialRepository
import app.cicada.data.database.CicadaDB
import app.cicada.security.CryptoManager
import app.cicada.security.VaultSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AutofillSaveCoordinator {

    private const val TAG = "AutofillSave"

    private data class PendingSave(
        val userId: String,
        val domain: String,
        val username: String,
        val password: String,
        val callback: SaveCallback
    )

    @Volatile
    private var pendingSave: PendingSave? = null

    @Synchronized
    fun begin(
        userId: String,
        domain: String,
        username: String,
        password: String,
        callback: SaveCallback
    ) {
        pendingSave?.callback?.onFailure(
            "The previous Cicada save request was interrupted."
        )

        pendingSave = PendingSave(
            userId = userId,
            domain = domain,
            username = username,
            password = password,
            callback = callback
        )
    }

    fun hasPendingSave(): Boolean = pendingSave != null

    suspend fun complete(
        context: Context,
        vaultSession: VaultSession
    ) {

        Log.d(
            "AutofillSaveDebug",
            "Coordinator complete: isUnlocked=${vaultSession.isUnlocked}"
        )

        val pending = synchronized(this) {
            pendingSave.also {
                pendingSave = null
            }
        } ?: return

        try {

            Log.d("AutofillSaveDebug", "About to read active user ID")

            val activeUserId = withContext(Dispatchers.IO) {
                vaultSession.getUserId()
            }

            check(activeUserId == pending.userId) {
                "The unlocked account does not match the save request."
            }

            val appContext = context.applicationContext
            val db = CicadaDB.getInstance(appContext)

            val repository = CredentialRepository(
                credentialDAO = db.credentialDAO(),
                cryptoManager = CryptoManager(),
                vaultSession = vaultSession
            )

            val existing = withContext(Dispatchers.IO) {
                db.credentialDAO().getCredentialsForDomain(
                    userId = pending.userId,
                    websiteDomain = pending.domain
                )
            }

            val matches = if (pending.username.isNotBlank()) {
                existing.filter {
                    it.username?.trim() == pending.username.trim()
                }
            } else {
                emptyList()
            }

            withContext(Dispatchers.IO) {
                when (matches.size) {
                    0 -> repository.addCredentialToVault(
                        title = pending.domain,
                        username = pending.username,
                        password = pending.password,
                        website = "https://${pending.domain}",
                        notes = ""
                    )

                    1 -> repository.updateCredential(
                        credentialId = matches.single().id,
                        title = pending.domain,
                        username = pending.username,
                        password = pending.password,
                        website = "https://${pending.domain}",
                        notes = ""
                    )

                    else -> error(
                        "Multiple accounts match this username and domain."
                    )
                }
            }

            Log.d(TAG, "Credential saved after authentication")
            pending.callback.onSuccess()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete pending save", e)
            pending.callback.onFailure(
                "Cicada couldn't save this login. Please unlock your vault and try again."
            )
        } finally {
            // Best-effort removal of references to the plaintext password.
            synchronized(this) {
                if (pendingSave === pending) {
                    pendingSave = null
                }
            }
        }
    }

    fun fail(message: String) {
        val pending = synchronized(this) {
            pendingSave.also {
                pendingSave = null
            }
        } ?: return

        pending.callback.onFailure(message)
    }
}