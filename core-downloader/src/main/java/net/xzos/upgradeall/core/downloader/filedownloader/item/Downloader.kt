package net.xzos.upgradeall.core.downloader.filedownloader.item

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.FetchGroup
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.util.DEFAULT_GROUP_ID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.downloader.downloadConfig
import net.xzos.upgradeall.core.downloader.filedownloader.*
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadRegister
import net.xzos.upgradeall.core.downloader.service.DownloadService
import net.xzos.upgradeall.core.utils.*
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.coroutines.ValueLock
import net.xzos.upgradeall.core.utils.coroutines.runWithLock
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import java.io.File
import java.util.*


/* 下载管理 */
class Downloader internal constructor(downloadDir: File) {

    lateinit var downloadId: DownloadId
    val downloadFile = DownloadFile(downloadDir)

    internal val downloadOb = DownloadOb({}, {}, {},
        completeFunc = { completeObserverFun(it) },
        cancelFunc = { cancelObserverFun(it) }, {})

    private val fetch by lazy { runBlocking { DownloadService.getFetch() } }

    private val requestList: MutableList<Request> = mutableListOf()
    private val completeObserverFun: ObserverFun<Download> = fun(_) {
        cancel()
    }

    private val cancelObserverFun: ObserverFun<Download> = fun(_) {
        delTask()
    }

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

    internal fun addTask(
        fileName: String, url: String,
        headers: Map<String, String> = mapOf(), cookies: Map<String, String> = mapOf()
    ) {
        if (url.isNotBlank()) {
            val request = makeRequest(fileName, url, headers, cookies)
            requestList.add(request)
        }
    }

    suspend fun getDownloadProgress(): Int {
        getFetchDownload(downloadId)?.run {
            return progress
        } ?: getFetchGroup(downloadId)?.run {
            return groupDownloadProgress
        } ?: return -1
    }

    suspend fun getDownloadList(): List<Download> {
        getFetchDownload(downloadId)?.run {
            return listOf(this)
        } ?: getFetchGroup(downloadId)?.run {
            return this.downloads
        } ?: return emptyList()
    }

    internal suspend fun start(
        taskStartedFun: (Int) -> Unit,
        taskStartFailedFun: (Throwable) -> Unit,
        vararg downloadOb: DownloadOb
    ) {
        if (requestList.isEmpty()) {
            throw DownloadCanceledError("no request list")
        }
        downloadId = if (requestList.size == 1)
            DownloadId(false, requestList[0].id)
        else
            DownloadId(true, groupId)
        var start = false
        val ended = CoroutinesCount(requestList.size)
        val mutex = Mutex()
        for (request in requestList) {
            request.groupId = downloadId.id
            fetch.enqueue(request, fun(request) {
                mutex.runWithLock {
                    if (start) return@runWithLock
                    start = true
                    downloadOb.forEach { register(it) }
                    register()
                    taskStartedFun(request.id)
                }
                ended.down()
            }, {
                taskStartFailedFun(DownloadFetchError(it))
                ended.down()
            })
        }
        ended.waitNum(0)
    }

    internal fun resume() {
        if (downloadId.isGroup)
            fetch.resumeGroup(downloadId.id)
        else
            fetch.resume(downloadId.id)
    }

    internal fun pause() {
        if (downloadId.isGroup)
            fetch.pauseGroup(downloadId.id)
        else
            fetch.pause(downloadId.id)
    }

    internal fun retry() {
        if (downloadId.isGroup) {
            fetch.getDownloadsInGroup(downloadId.id) {
                for (download in it) {
                    fetch.retry(download.id)
                }
            }
        } else
            fetch.retry(downloadId.id)
    }

    internal fun cancel() {
        if (downloadId.isGroup)
            fetch.cancelGroup(downloadId.id)
        else
            fetch.cancel(downloadId.id)
    }

    private fun delTask() {
        if (downloadId.isGroup)
            fetch.deleteGroup(downloadId.id)
        else
            fetch.delete(downloadId.id)
        downloadFile.delete()
        unregister()
    }

    private fun register() {
        DownloaderManager.addDownloader(this)
    }

    private fun unregister() {
        DownloaderManager.removeDownloader(this)
    }

    companion object {
        const val TAG = "Downloader"
        val logTagObject = ObjectTag(core, TAG)

        private val groupIdMutex = Mutex()

        private var groupId = DEFAULT_GROUP_ID + 1
            get() {
                return groupIdMutex.runWithLock {
                    field.also {
                        field += 1
                    }
                }
            }
    }

    private fun makeRequest(
        fileName: String, url: String,
        headers: Map<String, String> = mapOf(), cookies: Map<String, String> = mapOf()
    ): Request {
        // 检查重复任务
        val file = downloadFile.getFile(fileName)
        val request = Request(url, file.path)
        request.autoRetryMaxAttempts = downloadConfig.DOWNLOAD_AUTO_RETRY_MAX_ATTEMPTS
        for ((key, value) in headers) {
            request.addHeader(key, value)
        }
        if (cookies.isNotEmpty()) {
            var cookiesStr = ""
            for ((key, value) in cookies) {
                cookiesStr += "$key: $value; "
            }
            if (cookiesStr.isNotBlank()) {
                cookiesStr = cookiesStr.subSequence(0, cookiesStr.length - 2).toString()
                request.addHeader("Cookie", cookiesStr)
            }
        }
        return request
    }

    private suspend fun getFetchDownload(downloadId: DownloadId): Download? {
        if (downloadId.isGroup) return null
        val valueLock = ValueLock<Download?>()
        fetch.getDownload(downloadId.id) {
            valueLock.setValue(it)
        }
        return valueLock.getValue()
    }

    private suspend fun getFetchGroup(downloadId: DownloadId): FetchGroup? {
        if (!downloadId.isGroup) return null
        val valueLock = ValueLock<FetchGroup>()
        fetch.getFetchGroup(downloadId.id) {
            valueLock.setValue(it)
        }
        return valueLock.getValue()
    }
}