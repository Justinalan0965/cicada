package app.cicada.autofill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import app.cicada.data.credential.Credential
import app.cicada.data.database.CicadaDB
import app.cicada.security.BiometricAuthenticator
import app.cicada.security.BiometricKeyManager
import app.cicada.security.CryptoManager
import app.cicada.security.exception.BiometricKeyInvalidatedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AutofillAuthActivity : FragmentActivity() {

    companion object {
        const val EXTRA_CREDENTIAL_ID =
            "credential_id"

        const val EXTRA_USER_ID =
            "user_id"

        const val EXTRA_USERNAME_ID =
            "username_id"

        const val EXTRA_PASSWORD_ID =
            "password_id"

        const val EXTRA_PACKAGE_NAME =
            "package_name"

        const val EXTRA_WEB_DOMAIN =
            "web_domain"

        private const val TAG = "AutofillAuth"
    }

    private lateinit var db: CicadaDB
    private lateinit var cryptoManager: CryptoManager
    private lateinit var biometricAuthenticator: BiometricAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = CicadaDB.getInstance(applicationContext)
        cryptoManager = CryptoManager()
        biometricAuthenticator = BiometricAuthenticator(this)

        val credentialId =
            intent.getStringExtra(EXTRA_CREDENTIAL_ID)

        val userId =
            intent.getStringExtra(EXTRA_USER_ID)

        val usernameId =
            intent.getParcelableExtra<android.view.autofill.AutofillId>(
                EXTRA_USERNAME_ID
            )

        val passwordId =
            intent.getParcelableExtra<android.view.autofill.AutofillId>(
                EXTRA_PASSWORD_ID
            )

        val webDomain =
            intent.getStringExtra(EXTRA_WEB_DOMAIN)

        if (
            credentialId == null ||
            userId == null
        ) {
            Log.e(TAG, "Missing authentication data")
            finish()
            return
        }

        Log.d(TAG, "Authentication requested")

        authenticateAndFill(
            credentialId = credentialId,
            userId = userId,
            usernameId = usernameId,
            passwordId = passwordId,
            webDomain = webDomain
        )
    }

    private fun authenticateAndFill(
        credentialId: String,
        userId: String,
        usernameId: android.view.autofill.AutofillId?,
        passwordId: android.view.autofill.AutofillId?,
        webDomain: String?
    ) {
        lifecycleScope.launch {

            val keyManager = BiometricKeyManager(userId)

            try {
                val user = withContext(Dispatchers.IO) {
                    db.userDAO().getUserById(userId)
                }

                if (user == null) {
                    Log.e(TAG, "User not found")
                    finish()
                    return@launch
                }

                val encryptedBiometricKey =
                    user.encryptedBiometricKey

                if (encryptedBiometricKey == null) {
                    Log.e(TAG, "Biometric unlock is not enabled")
                    finish()
                    return@launch
                }


                if (!keyManager.hasKey()) {
                    Log.e(TAG, "Biometric keystore key not found")
                    finish()
                    return@launch
                }

                if (!biometricAuthenticator.canAuthenticate()) {
                    Log.e(TAG, "Biometric authentication unavailable")
                    finish()
                    return@launch
                }

                val cipher = keyManager.createDecryptCipher(
                    encryptedBiometricKey
                )

                biometricAuthenticator.authenticate(
                    cipher = cipher,
                    onSuccess = { authenticatedCipher ->

                        lifecycleScope.launch {
                            completeAuthentication(
                                credentialId = credentialId,
                                userId = userId,
                                usernameId = usernameId,
                                passwordId = passwordId,
                                webDomain = webDomain,
                                keyManager = keyManager,
                                encryptedBiometricKey =
                                    encryptedBiometricKey,
                                authenticatedCipher =
                                    authenticatedCipher
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.d(
                            TAG,
                            "Biometric authentication failed: $error"
                        )

                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                )

            } catch (e: BiometricKeyInvalidatedException) {
                Log.e(TAG, "Biometric key is invalidated", e)

                withContext(Dispatchers.IO) {
                    db.userDAO().updateBiometricKey(
                        userId,
                        null
                    )
                }

                keyManager.deleteKey()

                setResult(Activity.RESULT_CANCELED)
                finish()

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Failed to start Autofill authentication",
                    e
                )

                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private suspend fun completeAuthentication(
        credentialId: String,
        userId: String,
        usernameId: android.view.autofill.AutofillId?,
        passwordId: android.view.autofill.AutofillId?,
        webDomain: String?,
        keyManager: BiometricKeyManager,
        encryptedBiometricKey: ByteArray,
        authenticatedCipher: javax.crypto.Cipher
    ) {
        var vaultKey: ByteArray? = null

        try {
            vaultKey = withContext(Dispatchers.IO) {
                keyManager.decrypt(
                    authenticatedCipher,
                    encryptedBiometricKey
                )
            }

            val credentialEntity =
                withContext(Dispatchers.IO) {
                    db.credentialDAO()
                        .getCredentialById(
                            userId = userId,
                            credentialId = credentialId
                        )
                }

            if (credentialEntity == null) {
                Log.e(TAG, "Credential not found")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }

            if (
                credentialEntity.websiteDomain !=
                webDomain?.lowercase()?.removePrefix("www.")
            ) {
                Log.e(TAG, "Credential domain mismatch")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }

            val decryptedData =
                cryptoManager.decrypt(
                    credentialEntity.encryptedData,
                    vaultKey
                )

            try {
                val credential =
                    Json.decodeFromString<Credential>(
                        decryptedData.toString(Charsets.UTF_8)
                    )

                val presentation = RemoteViews(
                    packageName,
                    android.R.layout.simple_list_item_1
                )

                presentation.setTextViewText(
                    android.R.id.text1,
                    "Cicada • ${credential.username}"
                )

                val datasetBuilder =
                    android.service.autofill.Dataset.Builder(
                        presentation
                    )

                usernameId?.let { id ->
                    datasetBuilder.setValue(
                        id,
                        AutofillValue.forText(
                            credential.username
                        ),
                        presentation
                    )
                }

                passwordId?.let { id ->
                    datasetBuilder.setValue(
                        id,
                        AutofillValue.forText(
                            credential.password
                        ),
                        presentation
                    )
                }

                val dataset =
                    datasetBuilder.build()

                val resultIntent = Intent().apply {
                    putExtra(
                        AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                        dataset
                    )

                    putExtra(
                        AutofillManager
                            .EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET,
                        true
                    )
                }

                setResult(
                    Activity.RESULT_OK,
                    resultIntent
                )

            } finally {
                decryptedData.fill(0)
            }

        } catch (e: BiometricKeyInvalidatedException) {
            Log.e(TAG, "Biometric key was invalidated", e)

            withContext(Dispatchers.IO) {
                db.userDAO().updateBiometricKey(
                    userId,
                    null
                )
            }

            keyManager.deleteKey()

            setResult(Activity.RESULT_CANCELED)

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to decrypt Autofill credential",
                e
            )

            setResult(Activity.RESULT_CANCELED)

        } finally {
            vaultKey?.fill(0)
            finish()
        }
    }
}