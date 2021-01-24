package net.xzos.upgradeall.core.downloader

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.util.DEFAULT_GROUP_ID
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.core.utils.Func
import net.xzos.upgradeall.core.utils.FuncR
import net.xzos.upgradeall.core.utils.file.FileUtil
import net.xzos.upgradeall.core.utils.file.getFileByAutoRename
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.core.utils.runWithLock
import net.xzos.upgradeall.core.utils.wait
import java.io.File


/* 下载管理 */
class Downloader(val fileAsset: FileAsset) {

    var downloadId: DownloadId = DownloadId(false, -1)
    val downloadDir = FileUtil.getNewRandomNameFile(FileUtil.DOWNLOAD_CACHE_DIR)

    private val fetch = DownloadService.getFetch()

    private val requestList: MutableList<Request> = mutableListOf()
    private val completeObserverFun: ObserverFun<Download> = fun(_) {
        unregister()
        cancel()
    }

    private val cancelObserverFun: ObserverFun<Download> = fun(_) {
        delTask()
    }

    fun finalize() {
        delTask()
    }

    suspend fun getFileList(): List<File> {
        val mutex = Mutex(true)
        val fileList = mutableListOf<File>()
        if (downloadId.isGroup)
            fetch.getDownloadsInGroup(downloadId.id) {
                for (download in it) {
                    fileList.add(File(download.file))
                }
                mutex.unlock()
            }
        else
            fetch.getDownload(downloadId.id) {
                if (it != null) fileList.add(File(it.file))
                mutex.unlock()
            }
        mutex.wait()
        return fileList
    }

    fun removeFile() {
        delTask()
    }

    fun addTask(
            fileName: String, url: String,
            headers: Map<String, String> = mapOf(), cookies: Map<String, String> = mapOf()
    ) {
        if (url.isNotBlank()) {
            val request = makeRequest(fileName, url, headers, cookies)
            requestList.add(request)
        }
    }

    fun start(taskStartedFun: FuncR<Int>, taskStartFailedFun: Func, downloadOb: DownloadOb) {
        if (!downloadDir.exists())
            downloadDir.mkdirs()
        if (requestList.isEmpty()) {
            taskStartFailedFun.call()
            return
        }
        downloadId = if (requestList.size == 1)
            DownloadId(false, requestList[0].id)
        else
            DownloadId(true, groupId)
        var start = false
        val mutex = Mutex()
        for (request in requestList) {
            request.groupId = downloadId.id
            fetch.enqueue(request, fun(request) {
                mutex.runWithLock {
                    if (start) return@runWithLock
                    start = true
                    DownloadRegister.registerOb(downloadId, downloadOb)
                    register()
                    taskStartedFun.call(request.id)
                }
            }, fun(_) {
                taskStartFailedFun.call()
            })
        }
    }

    fun resume() {
        if (downloadId.isGroup)
            fetch.resumeGroup(downloadId.id)
        else
            fetch.resume(downloadId.id)
    }

    fun pause() {
        if (downloadId.isGroup)
            fetch.pauseGroup(downloadId.id)
        else
            fetch.pause(downloadId.id)
    }

    fun retry() {
        if (downloadId.isGroup) {
            fetch.getDownloadsInGroup(downloadId.id) {
                for (download in it) {
                    fetch.retry(download.id)
                }
            }
        } else
            fetch.retry(downloadId.id)
    }

    fun cancel() {
        if (downloadId.isGroup)
            fetch.cancelGroup(downloadId.id)
        else
            fetch.cancel(downloadId.id)
    }

    private fun delTask() {
        unregister()
        if (downloadId.isGroup)
            fetch.deleteGroup(downloadId.id)
        else
            fetch.delete(downloadId.id)
        downloadDir.delete()
    }

    private fun register() {
        DownloaderManager.setDownloader(this)
        // 下载状态监视功能注册
        val downloadOb = DownloadOb({}, {}, {},
                completeFunc = { completeObserverFun(it) },
                cancelFunc = { cancelObserverFun(it) }, {})

        DownloadRegister.registerOb(downloadId, downloadOb)
    }

    private fun unregister() {
        DownloaderManager.removeDownloader(this)
        DownloadRegister.unRegisterById(downloadId)
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
        val file = File(downloadDir, fileName).getFileByAutoRename()
        val filePath = file.path
        val request = Request(url, filePath)
        request.autoRetryMaxAttempts = coreConfig.download_auto_retry_max_attempts
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
}