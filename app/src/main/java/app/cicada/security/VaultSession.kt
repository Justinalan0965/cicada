package app.cicada.security

class VaultSession {
    private var vaultKey: ByteArray? = null
    private var vaultId: String? = null

    val isUnlocked: Boolean
        get() = vaultKey != null

    fun unlock(vaultId: String, key: ByteArray) {
        lock()
        this.vaultId = vaultId
        this.vaultKey = key.copyOf()
    }

    fun getVaultId() : String {
        return vaultId
            ?: throw IllegalStateException("Vault is locked")
    }

    fun getKey(): ByteArray {
        return vaultKey?.copyOf()
            ?: throw IllegalStateException("Vault is locked")
    }

    fun lock() {
        vaultKey?.fill(0)

        vaultKey = null
        vaultId = null
    }
}