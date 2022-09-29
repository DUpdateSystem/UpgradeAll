package net.xzos.upgradeall.core.downloader.filedownloader.item.data

import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadWorker
import java.io.File

internal data class TaskData(
    val name: String,
    val filePath: String,
    val url: String,
    val headers: MutableMap<String, String> = mutableMapOf(),
    var autoRetryMaxAttempts: Int = 0,
) {
    fun header(key: String, value: String) = this.apply {
        this.headers[key] = value
    }

    fun headers(headers: Map<String, String>) = this.apply {
        this.headers.putAll(headers)
    }

    fun autoRetryMaxAttempts(value: Int) = this.apply {
        this.autoRetryMaxAttempts = value
    }
}

internal fun TaskData.file() = File(filePath)

internal fun TaskData.manager(): DownloadWorker {
    return DownloadWorker(this)
}