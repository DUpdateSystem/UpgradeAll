package net.xzos.upgradeall.core.data

/**
 * Release information from a provider
 */
data class Release(
    val versionName: String,
    val versionCode: Long? = null,
    val releaseDate: String? = null,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null,
    val fileSize: Long? = null,
    val sha256: String? = null
)