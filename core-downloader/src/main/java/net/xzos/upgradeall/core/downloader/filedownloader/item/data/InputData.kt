package net.xzos.upgradeall.core.downloader.filedownloader.item.data

import net.xzos.upgradeall.core.downloader.downloadConfig
import java.io.File

class InputData(
    val name: String,
    val url: String,
    val headers: Map<String, String>,
    val cookies: Map<String, String>,
)

internal fun InputData.getTaskData(dir: File): TaskData {
    // 检查重复任务
    val file = File(dir, name)
    val request = TaskData(name, file.path, url)
        .autoRetryMaxAttempts(downloadConfig.DOWNLOAD_AUTO_RETRY_MAX_ATTEMPTS)
    for ((key, value) in headers) {
        request.header(key, value)
    }
    if (cookies.isNotEmpty()) {
        var cookiesStr = ""
        for ((key, value) in cookies) {
            cookiesStr += "$key: $value; "
        }
        if (cookiesStr.isNotBlank()) {
            cookiesStr = cookiesStr.subSequence(0, cookiesStr.length - 2).toString()
            request.header("Cookie", cookiesStr)
        }
    }
    return request
}