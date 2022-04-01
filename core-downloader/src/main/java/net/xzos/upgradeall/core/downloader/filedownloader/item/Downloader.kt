package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.downloader.downloadConfig
import net.xzos.upgradeall.core.downloader.filedownloader.DownloadCanceledError
import net.xzos.upgradeall.core.downloader.filedownloader.DownloaderManager
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadRegister
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import zlc.season.rxdownload4.manager.*
import zlc.season.rxdownload4.task.Task
import java.io.File


/* 下载管理 */
class Downloader(downloadDir: File) {

    lateinit var downloadId: DownloadId
    val downloadFile = DownloadFile(downloadDir)

    private val requestList: MutableList<DownloadInfoProxy> = mutableListOf()
    internal val taskList: MutableList<DownloadTaskWrapper> = mutableListOf()
    fun register(downloadOb: DownloadOb) {
        DownloadRegister.registerOb(downloadId, downloadOb)
    }

    fun unregister(downloadOb: DownloadOb) {
        DownloadRegister.unRegisterOb(downloadId, downloadOb)
    }

    fun removeFile() {
        delTask()
        downloadFile.delete()
    }

    fun addTask(downloadInfoItem: DownloadInfoItem) {
        requestList.add(downloadInfoItem.getProxy())
    }

    fun getDownloadProgress(): Long {
        var totalSize = 0L
        var downloadedSize = 0L
        taskList.forEach {
            val status = it.snap ?: return@forEach
            totalSize += status.totalSize
            downloadedSize += status.downloadSize
        }
        return downloadedSize / totalSize * 100
    }

    fun getStatusList(): List<DownloadStatusSnap> {
        return taskList.mapNotNull { it.snap }
    }

    fun start(
        taskStartedFun: (DownloadId) -> Unit,
        taskStartFailedFun: (Throwable) -> Unit,
        vararg downloadOb: DownloadOb
    ) {
        if (requestList.isEmpty()) {
            taskStartFailedFun(DownloadCanceledError("no request list"))
            return
        }
        downloadId = DownloadId(requestList.size != 1, downloadIdCount.up())
        taskList.addAll(requestList.map {
            it.manager().let { manager ->
                DownloadTaskWrapper(manager).apply {
                    manager.start()
                }
            }
        })
        requestList.clear()
        register(*downloadOb)
        taskStartedFun(downloadId)
    }

    fun resume() {
        taskList.forEach { it.manager.start() }
    }

    fun pause() {
        taskList.forEach { it.manager.stop() }
    }

    fun retry() {
        taskList.forEach { it.manager.start() }
    }

    fun cancel() {
        taskList.forEach { it.manager.delete() }
    }

    private fun delTask() {
        taskList.forEach { it.manager.delete() }
        downloadFile.delete()
        unregister()
    }

    private fun register(vararg downloadOb: DownloadOb) {
        DownloaderManager.addDownloader(this)
        downloadOb.forEach {
            DownloadRegister.registerOb(downloadId, it)
        }
    }

    private fun unregister() {
        DownloaderManager.removeDownloader(this)
    }

    private fun DownloadInfoProxy.manager(): TaskManager {
        val task = Task(url, name, saveName = name, savePath = filePath)
        return task.manager(headers)
    }

    private fun DownloadInfoItem.getProxy(): DownloadInfoProxy {
        // 检查重复任务
        val file = downloadFile.getFile(name)
        val request = DownloadInfoProxy(name, file.path, url)
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

    companion object {
        const val TAG = "Downloader"
        val logTagObject = ObjectTag(core, TAG)

        private var downloadIdCount = CoroutinesCount(1)
    }
}