
    package app.cicada.autofill

    import android.app.Activity
    import android.content.Intent
    import android.os.Bundle
    import android.util.Log
    import android.widget.Toast
    import androidx.fragment.app.FragmentActivity
    import androidx.lifecycle.lifecycleScope
    import app.cicada.CicadaApplication
    import app.cicada.MainActivity
    import app.cicada.data.database.CicadaDB
    import app.cicada.data.preferences.UserPreferencesRepository
    import app.cicada.security.BiometricAuthenticator
    import app.cicada.security.BiometricKeyManager
    import app.cicada.security.VaultSession
    import app.cicada.security.exception.BiometricKeyInvalidatedException
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext

    class AutofillSaveAuthActivity : FragmentActivity() {

        companion object {
            const val EXTRA_USER_ID = "save_auth_user_id"
            private const val TAG = "AutofillSaveAuth"
        }

        private lateinit var vaultSession: VaultSession
        private lateinit var db: CicadaDB
        private lateinit var biometricAuthenticator: BiometricAuthenticator

        private var requestedUserId: String? = null
        private var waitingForManualUnlock = false
        private var saveCompleted = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            vaultSession =
                (application as CicadaApplication).vaultSession
            db = CicadaDB.getInstance(applicationContext)
            biometricAuthenticator = BiometricAuthenticator(this)

            requestedUserId = intent.getStringExtra(EXTRA_USER_ID)

            val userId = requestedUserId
            if (userId.isNullOrBlank()) {
                AutofillSaveCoordinator.fail(
                    "Missing account information. Please retry saving."
                )
                finish()
                return
            }

            if (isCorrectVaultUnlocked(userId)) {
                completePendingSave()
            } else {
                authenticateBiometrically(userId)
            }
        }

        override fun onResume() {
            super.onResume()

            val userId = requestedUserId ?: return

            if (
                waitingForManualUnlock &&
                !saveCompleted &&
                isCorrectVaultUnlocked(userId)
            ) {
                waitingForManualUnlock = false
                completePendingSave()
            }
        }

        private fun isCorrectVaultUnlocked(userId: String): Boolean {
            if (!vaultSession.isUnlocked || vaultSession.shouldAutoLock()) {
                if (vaultSession.shouldAutoLock()) {
                    vaultSession.lock()
                }
                return false
            }

            return runCatching {
                vaultSession.getUserId() == userId
            }.getOrDefault(false)
        }

        private fun authenticateBiometrically(userId: String) {
            lifecycleScope.launch {
                val keyManager = BiometricKeyManager(userId)

                try {
                    val user = withContext(Dispatchers.IO) {
                        db.userDAO().getUserById(userId)
                    }

                    val encryptedBiometricKey =
                        user?.encryptedBiometricKey

                    if (
                        encryptedBiometricKey == null ||
                        !keyManager.hasKey() ||
                        !biometricAuthenticator.canAuthenticate()
                    ) {
                        requestManualUnlock()
                        return@launch
                    }

                    val cipher = keyManager.createDecryptCipher(
                        encryptedBiometricKey
                    )

                    biometricAuthenticator.authenticate(
                        cipher = cipher,
                        onSuccess = { authenticatedCipher ->
                            lifecycleScope.launch {
                                var vaultKey: ByteArray? = null

                                try {
                                    vaultKey = withContext(Dispatchers.IO) {
                                        keyManager.decrypt(
                                            authenticatedCipher,
                                            encryptedBiometricKey
                                        )
                                    }

                                    Log.d("AutofillSaveDebug", "Biometric authentication succeeded")

                                    Log.d(
                                        "AutofillSaveDebug",
                                        "Before unlock: isUnlocked=${vaultSession.isUnlocked}"
                                    )

                                    vaultSession.unlock(
                                        userId = userId,
                                        key = vaultKey
                                    )

                                    Log.d(
                                        "AutofillSaveDebug",
                                        "After unlock: isUnlocked=${vaultSession.isUnlocked}"
                                    )

                                    completePendingSave()

                                } catch (e: Exception) {
                                    Log.e(
                                        TAG,
                                        "Failed to unlock vault for save",
                                        e
                                    )
                                    AutofillSaveCoordinator.fail(
                                        "Authentication succeeded, but Cicada couldn't unlock the vault."
                                    )
                                    finish()
                                } finally {
                                    vaultKey?.fill(0)
                                }
                            }
                        },
                        onFailure = { error ->
                            Log.d(
                                TAG,
                                "Save authentication failed: $error"
                            )
                            AutofillSaveCoordinator.fail(
                                "Authentication was cancelled or failed. Nothing was saved."
                            )
                            finish()
                        }
                    )

                } catch (e: BiometricKeyInvalidatedException) {
                    Log.e(TAG, "Biometric key invalidated", e)

                    withContext(Dispatchers.IO) {
                        db.userDAO().updateBiometricKey(userId, null)
                    }
                    keyManager.deleteKey()

                    requestManualUnlock()

                } catch (e: Exception) {
                    Log.e(TAG, "Unable to start save authentication", e)
                    requestManualUnlock()
                }
            }
        }

        private fun requestManualUnlock() {
            waitingForManualUnlock = false

            AutofillSaveCoordinator.fail(
                "Biometric unlock is unavailable. Open Cicada and unlock your vault, then try saving again."
            )

            finish()
        }

        private fun completePendingSave() {
            if (saveCompleted) return
            saveCompleted = true

            lifecycleScope.launch {
                try {
                    AutofillSaveCoordinator.complete(
                        context = applicationContext,
                        vaultSession = vaultSession
                    )
                } finally {
                    finish()
                }
            }
        }

        override fun onBackPressed() {
            AutofillSaveCoordinator.fail(
                "Save cancelled. No credential was changed."
            )
            super.onBackPressed()
        }
    }
