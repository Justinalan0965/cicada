package app.cicada.data.user

data class UnlockResult (
    val userId : String,
    val vaultKey : ByteArray
)