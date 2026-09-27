package app.cicada.security.exception

class BiometricKeyInvalidatedException (
    cause: Throwable
): Exception(
    "Biometric security key is no longer valid",
    cause
)