package net.xzos.upgradeall.core.downloader.filedownloader.item.data

import zlc.season.rxdownload4.manager.TaskManager
import zlc.season.rxdownload4.manager.manager
import zlc.season.rxdownload4.task.Task

internal data class TaskData(
    val name: String,
    val filePath: String,
    val url: String,
    val headers: MutableMap<String, String> = mutableMapOf(),
    var autoRetryMaxAttempts: Int? = null,
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

internal fun TaskData.manager(): TaskManager {
    val task = Task(url, name, saveName = name, savePath = filePath)
    return task.manager(headers)
}