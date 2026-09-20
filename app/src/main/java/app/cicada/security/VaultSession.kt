package app.cicada.security

class VaultSession {

    companion object {
        private const val AUTO_LOCK_TIMEOUT_MS = 5 * 60 * 1000L
    }

    private var userId: String? = null
    private var vaultKey: ByteArray? = null
    private var lastActivityTime: Long = 0L

    val isUnlocked: Boolean
        get() = vaultKey != null

    fun unlock(
        userId: String,
        key: ByteArray
    ) {
        lock()

        this.userId = userId
        this.vaultKey = key.copyOf()
        this.lastActivityTime = System.currentTimeMillis()
    }

    fun recordActivity() {
        if (isUnlocked) {
            lastActivityTime = System.currentTimeMillis()
        }
    }

    fun shouldAutoLock(): Boolean {
        if (!isUnlocked) {
            return false
        }

        val elapsed = System.currentTimeMillis() - lastActivityTime

        return elapsed >= AUTO_LOCK_TIMEOUT_MS
    }

    fun getUserId(): String {
        recordActivity()

        return userId
            ?: throw IllegalStateException("Vault is locked")
    }

    fun getKey(): ByteArray {
        recordActivity()

        return vaultKey?.copyOf()
            ?: throw IllegalStateException("Vault is locked")
    }

    fun lock() {
        vaultKey?.fill(0)

        vaultKey = null
        userId = null
        lastActivityTime = 0L
    }
}