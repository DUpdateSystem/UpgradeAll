package net.xzos.upgradeall.core.downloader.filedownloader.item

data class DownloadInfoProxy(
    val name: String,
    val filePath: String,
    val url: String,
    val headers: MutableMap<String, String> = mutableMapOf(),
    var autoRetryMaxAttempts: Int? = null,
) {
    fun header(key: String, value: String): DownloadInfoProxy {
        this.headers[key] = value
        return this
    }

    fun headers(headers: Map<String, String>): DownloadInfoProxy {
        this.headers.putAll(headers)
        return this
    }

    fun autoRetryMaxAttempts(value: Int): DownloadInfoProxy {
        this.autoRetryMaxAttempts = value
        return this
    }
}