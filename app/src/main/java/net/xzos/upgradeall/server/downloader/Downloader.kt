package net.xzos.upgradeall.server.downloader

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2.util.DEFAULT_GROUP_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.json.gson.DownloadInfoItem
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.server.downloader.DownloadRegister.getCancelNotifyKey
import net.xzos.upgradeall.server.downloader.DownloadRegister.getCompleteNotifyKey
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.activity.file_pref.SaveFileActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.file.FileUtil
import net.xzos.upgradeall.utils.install.ApkInstaller
import net.xzos.upgradeall.utils.install.autoAddApkExtension
import net.xzos.upgradeall.utils.install.isApkFile
import java.io.File


class Downloader(private val context: Context) {

    private val requestList: MutableList<Request> = mutableListOf()
    private var downloadId = DEFAULT_GROUP_ID
    private val downloadNotification: DownloadNotification by lazy { DownloadNotification(downloadId) }

    private val completeObserverFun: ObserverFun<Download> = fun(_) {
        taskComplete()
        unregister()
    }

    private val cancelObserverFun: ObserverFun<Download> = fun(_) {
        delTask()
    }

    fun finalize() {
        delTask()
    }

    private fun register() {
        val registerDownloadMap = setDownloader(downloadId, this)
        // 若下载器组成成功，进行下载状态监视功能注册
        if (registerDownloadMap) {
            downloadNotification.register()
            DownloadRegister.observeForever(downloadId.getCompleteNotifyKey(), completeObserverFun)
            DownloadRegister.observeForever(downloadId.getCancelNotifyKey(), cancelObserverFun)
        }
    }

    private fun unregister() {
        DownloadRegister.removeObserver(completeObserverFun)
        DownloadRegister.removeObserver(cancelObserverFun)
    }

    private fun delDownloader() {
        DOWNLOADER_MAP.remove(downloadId)
    }

    fun addTask(fileName: String, url: String,
                headers: Map<String, String> = mapOf(), cookies: Map<String, String> = mapOf()) {
        val request = makeRequest(fileName, url, headers, cookies)
        requestList.add(request)
    }

    fun start(registerFun: (downloadId: Int) -> Unit) {
        if (requestList.isEmpty()) return
        val groupId = if (requestList.size == 1) {
            downloadId = requestList[0].id
            DEFAULT_GROUP_ID
        } else {
            groupId.also {
                downloadId = it
            }
        }
        for (request in requestList) {
            request.groupId = groupId
            fetch.enqueue(request, fun(request) {
                val file = File(request.file)
                downloadNotification.waitDownloadTaskNotification(file.name)
                val text = file.name + context.getString(R.string.download_task_begin)
                MiscellaneousUtils.showToast(text)
                register()
                registerFun(request.id)
            }, fun(_) {
                val file = File(request.file)
                MiscellaneousUtils.showToast(text = "下载失败: ${file.name}")
            })
        }
    }

    fun resume() {
        fetch.resume(downloadId)
    }

    fun pause() {
        fetch.pause(downloadId)
    }

    fun retry() {
        fetch.retry(downloadId)
    }

    private fun cancel() {
        fetch.cancel(downloadId)
    }

    fun delTask() {
        unregister()
        delDownloader()
        fetch.delete(downloadId)
    }

    fun install() {
        val file = File(requestList[0].file)
        GlobalScope.launch(Dispatchers.IO) {
            if (requestList.size > 1) {
                ApkInstaller.multipleInstall(requestList.map { File(it.file) }, fun(_) {
                    completeInstall()
                })
            } else {
                when {
                    file.isApkFile() -> {
                        downloadNotification.showInstallNotification(file.name)
                        ApkInstaller.install(file, fun(_) {
                            completeInstall()
                        })
                    }
                    else -> return@launch
                }
            }
        }
    }

    fun saveFile() {
        for (request in requestList) {
            val file = File(request.file)
            val mimeType = FileUtil.getMimeTypeByUri(context, Uri.fromFile(file))
            GlobalScope.launch {
                SaveFileActivity.newInstance(
                        file.name, mimeType,
                        file.readBytes(), context
                )
            }
        }
    }

    private fun makeRequest(fileName: String, url: String,
                            headers: Map<String, String> = mapOf(), cookies: Map<String, String> = mapOf()
    ): Request {
        // 检查重复任务
        val file = File(downloadDir, fileName)
        val filePath = file.path
        val request = Request(url, filePath)
        request.autoRetryMaxAttempts = PreferencesMap.download_auto_retry_max_attempts
        if (headers.isNotEmpty())
            for ((key, value) in headers) {
                request.addHeader(key, value)
            }
        if (cookies.isNotEmpty()) {
            var cookiesStr = ""
            for ((key, value) in headers) {
                cookiesStr += "$key: $value; "
            }
            if (cookiesStr.isNotBlank()) {
                cookiesStr = cookiesStr.subSequence(0, cookiesStr.length - 2).toString()
                request.addHeader("Cookie", cookiesStr)
            }
        }
        return request
    }

    private fun taskComplete() {
        cancel()
        for (request in requestList) {
            val file = File(request.file).autoAddApkExtension()
            // 自动转储
            FileUtil.DOWNLOAD_DOCUMENT_FILE?.let {
                FileUtil.dumpFile(file, it)
            }
        }
        // 自动安装
        if (PreferencesMap.auto_install) {
            install()
        }
    }

    private fun completeInstall() {
        delTask()
        UpdateService.startService(context, false)
        downloadNotification.taskCancel()  // 手动取消通知，因下载完成通知已解绑
        if (PreferencesMap.auto_delete_file) {
            requestList.map { File(it.file) }.forEach { file ->
                file.delete()
            }
            MiscellaneousUtils.showToast(R.string.auto_deleted_file)
        }
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var fetch: Fetch

        const val TAG = "Downloader"
        val logTagObject = ObjectTag(core, TAG)

        init {
            renewFetch(MyApplication.context)
        }

        fun renewFetch(context: Context) {
            val fetchConfiguration = FetchConfiguration.Builder(context)
                    .setDownloadConcurrentLimit(PreferencesMap.download_max_task_num)
                    .setHttpDownloader(getDownloader())
                    .build()
            fetch = Fetch.Impl.getInstance(fetchConfiguration)
            fetch.addListener(DownloadRegister)
        }

        private val downloadDir = FileUtil.DOWNLOAD_CACHE_DIR

        private val DOWNLOADER_MAP: HashMap<Int, Downloader> = hashMapOf()
        internal fun getDownloader(downloadId: Int): Downloader? = DOWNLOADER_MAP[downloadId]
        private fun setDownloader(downloadId: Int, downloader: Downloader): Boolean {
            return if (DOWNLOADER_MAP.containsKey(downloadId)) {
                false
            } else {
                DOWNLOADER_MAP[downloadId] = downloader
                true
            }
        }

        fun startDownloadService(downloadInfoList: List<DownloadInfoItem>, context: Context) {
            DownloadService.startService(context, downloadInfoList)
        }
    }

    private var groupId = DEFAULT_GROUP_ID
        get() {
            field += 1
            return field
        }
}
