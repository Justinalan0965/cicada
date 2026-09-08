package app.cicada.data.credential

import kotlinx.serialization.Serializable


@Serializable
data class Credential(
    val id: String,
    val title: String,
    val username: String,
    val password: String,
    val website: String?,
    val notes: String?
)