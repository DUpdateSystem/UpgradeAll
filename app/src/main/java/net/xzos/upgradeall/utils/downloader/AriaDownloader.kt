package net.xzos.upgradeall.utils.downloader

import android.annotation.SuppressLint
import android.net.Uri
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.common.HttpOption
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.task.DownloadTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data_manager.utils.FilePathUtils
import net.xzos.upgradeall.core.oberver.ObserverFun
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.server.update.UpdateService
import net.xzos.upgradeall.ui.activity.file_pref.SaveFileActivity
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.downloader.AriaRegister.getCancelNotifyKey
import net.xzos.upgradeall.utils.downloader.AriaRegister.getCompleteNotifyKey
import net.xzos.upgradeall.utils.install.ApkInstaller
import net.xzos.upgradeall.utils.install.autoAddApkExtension
import net.xzos.upgradeall.utils.install.isApkFile
import java.io.File


class AriaDownloader(private val url: String) {

    private var taskId: Long = -1L
    private var downloadFile: File? = null
    private val downloadNotification: DownloadNotification = DownloadNotification(url)

    private val completeObserverFun: ObserverFun<DownloadTask> = fun(downloadTask) {
        taskComplete(downloadTask)
        unregister()
    }

    private val cancelObserverFun: ObserverFun<DownloadTask> = fun(_) {
        delTask()
    }

    fun finalize() {
        delTask()
    }

    private fun register() {
        val registerDownloadMap = downloaderMap.setDownloader(url, this)
        // 若下载器组成成功，进行下载状态监视功能注册
        if (registerDownloadMap) {
            downloadNotification.register()
            AriaRegister.observeForever(url.getCompleteNotifyKey(), completeObserverFun)
            AriaRegister.observeForever(url.getCancelNotifyKey(), cancelObserverFun)
        }
    }

    private fun unregister() {
        AriaRegister.removeObserver(completeObserverFun)
        AriaRegister.removeObserver(cancelObserverFun)
    }

    private fun delDownloader() {
        downloaderMap.remove(url)
    }

    suspend fun start(fileName: String, headers: HashMap<String, String> = hashMapOf()): File? {
        return mutex.withLock {
            startAndRegister(fileName, headers)
        }
    }

    fun resume() {
        Aria.download(this).load(taskId)
                .ignoreCheckPermissions()
                .resume()
    }

    fun stop() {
        Aria.download(this).load(taskId)
                .ignoreCheckPermissions()
                .stop()
    }

    fun restart() {
        Aria.download(this).load(taskId)
                .ignoreCheckPermissions()
                .reStart()
    }

    private fun cancel() {
        Aria.download(this).load(taskId)
                .ignoreCheckPermissions()
                .cancel(false)
    }

    fun delTask() {
        unregister()
        delDownloader()
        Aria.download(this).load(taskId)
                .ignoreCheckPermissions()
                .cancel(true)
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
                    this@AriaDownloader.downloadFile!!.name, this@AriaDownloader.downloadFile!!.readBytes(),
                    mimeType, context
            )
        }
    }

    private fun startAndRegister(fileName: String, headers: Map<String, String> = hashMapOf()): File? {
        val file = startDownloadTask(fileName, headers)
        if (file != null) {
            downloadFile = file
            downloadNotification.waitDownloadTaskNotification(file.name)
            val text = file.name + context.getString(R.string.download_task_begin)
            MiscellaneousUtils.showToast(context, text = text)
            register()
        } else {
            MiscellaneousUtils.showToast(context, R.string.repeated_download_task)
        }
        return file
    }

    private fun startDownloadTask(fileName: String, headers: Map<String, String>): File? {
        // 检查重复任务
        val downloader = getDownloader(url)
        val taskExists = Aria.download(this).taskExists(url)
        when {
            downloader != null && taskExists -> {
                // 下载未完成
                // 继续 并返回已有任务文件
                downloader.resume()
                val filePath = Aria.download(this).getDownloadEntity(taskId)?.filePath
                        ?: return null
                return File(filePath)
            }
            downloader != null && !taskExists -> {
                // 下载已完成或正在等待开始下载
                return downloader.downloadFile
            }
            downloader == null && taskExists -> {
                // 下载未完成且中途因不明原因被终止
                val taskId = Aria.download(this).load(url).entity.id
                Aria.download(this).load(taskId).cancel(true)
            }
        }
        // 检查下载文件列表
        val taskList = Aria.download(this).totalTaskList
        val taskFileList = mutableListOf<File>()
        for (task in taskList) {
            task as DownloadEntity
            taskFileList.add(File(task.filePath))
        }
        val downloadFile = FilePathUtils.renameSameFile(
                File(downloadDir, fileName), taskFileList
        )
        val option = HttpOption()
        if (headers.isNotEmpty())
            option.addHeaders(headers)
        taskId = Aria.download(this)
                .load(url)
                .setFilePath(downloadFile.path)
                .option(option)
                .ignoreCheckPermissions()
                .create()
        return downloadFile
    }

    fun taskComplete(task: DownloadTask) {
        cancel()
        val file = File(task.filePath).autoAddApkExtension().also {
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
        UpdateService.startService(context)
        downloadNotification.taskCancel()  // 手动取消通知，因下载完成通知已解绑
        if (PreferencesMap.auto_delete_file) {
            file.delete()
            MiscellaneousUtils.showToast(context, R.string.auto_deleted_file)
        }
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        private val context = MyApplication.context
        private val mutex = Mutex()

        private val downloadDir = FileUtil.DOWNLOAD_CACHE_DIR

        private val downloaderMap: HashMap<String, AriaDownloader> = hashMapOf()
        internal fun getDownloader(url: String): AriaDownloader? = downloaderMap[url]
        private fun HashMap<String, AriaDownloader>.setDownloader(url: String, downloader: AriaDownloader): Boolean {
            return if (this.containsKey(url)) {
                false
            } else {
                this[url] = downloader
                true
            }
        }

        fun startDownloadService(url: String, fileName: String, headers: HashMap<String, String> = hashMapOf()) {
            AriaDownloadService.startService(context, url, fileName, headers)
        }
    }
}
