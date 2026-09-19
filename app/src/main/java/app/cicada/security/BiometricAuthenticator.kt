package app.cicada.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

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
        onSuccess: () -> Unit,
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

                    onSuccess()
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

                    // This means the biometric didn't match.
                    // The prompt can remain open and allow another attempt.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Cicada")
            .setSubtitle("Authenticate to unlock your vault")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}