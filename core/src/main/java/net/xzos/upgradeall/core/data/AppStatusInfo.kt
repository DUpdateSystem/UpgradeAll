package net.xzos.upgradeall.core.data

/**
 * Data class for app status information from Rust getter
 */
data class AppStatusInfo(
    val appId: String,
    val status: String,
    val currentVersion: String?,
    val latestVersion: String?
) {
    companion object {
        // Status constants matching Rust AppStatus enum
        const val STATUS_INACTIVE = "AppInactive"
        const val STATUS_PENDING = "AppPending"
        const val STATUS_NETWORK_ERROR = "NetworkError"
        const val STATUS_LATEST = "AppLatest"
        const val STATUS_OUTDATED = "AppOutdated"
        const val STATUS_NO_LOCAL = "AppNoLocal"
    }
}