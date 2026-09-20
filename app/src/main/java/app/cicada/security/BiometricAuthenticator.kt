package app.cicada.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

class BiometricAuthenticator(
    private val context: Context
) {

    private val biometricManager =
        BiometricManager.from(context)

    fun canAuthenticate(): Boolean {
        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val activity = context as? FragmentActivity
            ?: throw IllegalStateException(
                "Biometric authentication requires FragmentActivity"
            )

        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    val authenticatedCipher =
                        result.cryptoObject?.cipher

                    if (authenticatedCipher != null) {
                        onSuccess(authenticatedCipher)
                    } else {
                        onFailure(
                            "Biometric authentication succeeded, " +
                                "but cryptographic operation was unavailable"
                        )
                    }
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(
                        errorCode,
                        errString
                    )

                    onFailure(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    // Don't close the operation.
                    // Android allows another biometric attempt.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Cicada")
            .setSubtitle("Authenticate to unlock your vault")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipher)
        )
    }
}