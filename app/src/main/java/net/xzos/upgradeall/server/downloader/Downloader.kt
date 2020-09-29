package net.xzos.upgradeall.server.downloader

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.tonyodev.fetch2.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag.Companion.core
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.server.downloader.AriaRegister.getCancelNotifyKey
import net.xzos.upgradeall.server.downloader.AriaRegister.getCompleteNotifyKey
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.activity.file_pref.SaveFileActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.file.FileUtil
import net.xzos.upgradeall.utils.install.ApkInstaller
import net.xzos.upgradeall.utils.install.autoAddApkExtension
import net.xzos.upgradeall.utils.install.isApkFile
import java.io.File


class AriaDownloader(private val url: String, private val context: Context) {

    private var downloadFile: File? = null
    private lateinit var request: Request
    private val downloadNotification: DownloadNotification by lazy { DownloadNotification(request.id) }

    private val completeObserverFun: ObserverFun<Download> = fun(downloadTask) {
        taskComplete(downloadTask)
        unregister()
    }

    private val cancelObserverFun: ObserverFun<Download> = fun(_) {
        delTask()
    }

    fun finalize() {
        delTask()
    }

    private fun register() {
        val registerDownloadMap = setDownloader(request.id, this)
        // 若下载器组成成功，进行下载状态监视功能注册
        if (registerDownloadMap) {
            downloadNotification.register()
            AriaRegister.observeForever(request.id.getCompleteNotifyKey(), completeObserverFun)
            AriaRegister.observeForever(request.id.getCancelNotifyKey(), cancelObserverFun)
        }
    }

    private fun unregister() {
        AriaRegister.removeObserver(completeObserverFun)
        AriaRegister.removeObserver(cancelObserverFun)
    }

    private fun delDownloader() {
        downloaderMap.remove(request.id)
    }

    fun start(fileName: String, headers: HashMap<String, String> = hashMapOf(), registerFun: (downloadId: Int) -> Unit) {
        startDownloadTask(fileName, headers, fun(request) {
            val file = File(request.file)
            downloadNotification.waitDownloadTaskNotification(file.name)
            val text = file.name + context.getString(R.string.download_task_begin)
            MiscellaneousUtils.showToast(text)
            register()
            registerFun(request.id)
        }, fun(_) {
            MiscellaneousUtils.showToast(text = "下载失败: $fileName")
        })
    }

    fun resume() {
        fetch.resume(request.id)
    }

    fun pause() {
        fetch.pause(request.id)
    }

    fun restart() {
        fetch.retry(request.id)
    }

    private fun cancel() {
        fetch.cancel(request.id)
    }

    fun delTask() {
        unregister()
        delDownloader()
        fetch.delete(request.id)
    }

    fun install() {
        val file = downloadFile ?: return
        downloadNotification.showInstallNotification(file.name)
        when {
            file.isApkFile() -> {
                ApkInstaller.observeInstall(file, fun(_) {
                    completeInstall(file)
                })
                GlobalScope.launch { ApkInstaller.install(file) }
            }
            else -> return
        }
    }

    fun saveFile() {
        if (downloadFile == null) return
        val mimeType = FileUtil.getMimeTypeByUri(context, Uri.fromFile(this.downloadFile))
        GlobalScope.launch {
            SaveFileActivity.newInstance(
                    this@AriaDownloader.downloadFile!!.name, mimeType,
                    this@AriaDownloader.downloadFile!!.readBytes(), context
            )
        }
    }

    private fun startDownloadTask(fileName: String, headers: Map<String, String>,
                                  startFun: (_: Request) -> Unit, errorFun: (_: Error) -> Unit) {
        // 检查重复任务
        val file = File(downloadDir, fileName)
        val filePath = file.path
        request = Request(url, filePath)
        request.autoRetryMaxAttempts = PreferencesMap.download_auto_retry_max_attempts
        if (headers.isNotEmpty())
            for ((key, value) in headers) {
                request.addHeader(key, value)
            }
        fetch.enqueue(request = request, func = startFun, func2 = errorFun)
    }

    private fun taskComplete(task: Download) {
        cancel()
        val file = File(task.file).autoAddApkExtension().also {
            downloadFile = it
        }
        // 自动转储
        FileUtil.DOWNLOAD_DOCUMENT_FILE?.let {
            FileUtil.dumpFile(file, it)
        }
        // 自动安装
        if (PreferencesMap.auto_install) {
            install()
        }
    }

    private fun completeInstall(file: File) {
        delTask()
        UpdateService.startService(context, false)
        downloadNotification.taskCancel()  // 手动取消通知，因下载完成通知已解绑
        if (PreferencesMap.auto_delete_file) {
            file.delete()
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
            fetch.addListener(AriaRegister)
        }

        private val downloadDir = FileUtil.DOWNLOAD_CACHE_DIR

        private val downloaderMap: HashMap<Int, AriaDownloader> = hashMapOf()
        internal fun getDownloader(downloadId: Int): AriaDownloader? = downloaderMap[downloadId]
        private fun setDownloader(downloadId: Int, downloader: AriaDownloader): Boolean {
            return if (downloaderMap.containsKey(downloadId)) {
                false
            } else {
                downloaderMap[downloadId] = downloader
                true
            }
        }

        fun startDownloadService(url: String, fileName: String, headers: Map<String, String> = hashMapOf(), context: Context) {
            AriaDownloadService.startService(context, url, fileName, headers)
        }
    }
}
