package net.xzos.upgradeall.utils.downloader

import android.annotation.SuppressLint
import android.net.Uri
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.common.HttpOption
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.task.DownloadTask
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data_manager.utils.FilePathUtils
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.data.PreferencesMap
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

    private val completeObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return taskComplete(vars[0] as DownloadTask).also {
                unregister()
            }
        }
    }

    private val cancelObserver = object : Observer {
        override fun onChanged(vars: Array<out Any>): Any? {
            return unregister()
        }
    }

    fun finalize() {
        unregister()
    }

    private fun register() {
        downloaderMap[url] = this
        AriaRegister.observeForever(url.getCompleteNotifyKey(), completeObserver)
        AriaRegister.observeForever(url.getCancelNotifyKey(), cancelObserver)
    }

    private fun unregister() {
        downloaderMap.remove(url)
        AriaRegister.removeObserver(url.getCompleteNotifyKey())
        AriaRegister.removeObserver(url.getCancelNotifyKey())
    }

    fun start(fileName: String, headers: Map<String, String> = hashMapOf()): File? {
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
        Aria.download(this).load(taskId)
                .ignoreCheckPermissions()
                .cancel(true)
    }

    fun install() {
        val file = downloadFile ?: return
        when {
            file.isApkFile() -> GlobalScope.launch { ApkInstaller.install(file) }
            else -> return
        }
        downloadNotification.showInstallNotification(file.name)
        ApkInstaller.observeInstall(file, object : Observer {
            override fun onChanged(vararg vars: Any): Any? {
                return completeInstall(file)
            }
        })
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

    private fun startDownloadTask(fileName: String, headers: Map<String, String>): File? {
        // 检查重复任务
        val downloader = getDownloader(url)
        if (downloader != null) {
            // 继续 并返回已有任务文件
            downloader.resume()
            val filePath = Aria.download(this).getDownloadEntity(taskId)?.filePath ?: return null
            return File(filePath)
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
        downloadNotification.taskCancel()
        if (PreferencesMap.auto_delete_file) {
            file.delete()
            MiscellaneousUtils.showToast(context, R.string.auto_deleted_file)
        }
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        private val context = MyApplication.context

        private val downloadDir = FileUtil.DOWNLOAD_CACHE_DIR

        private val downloaderMap: HashMap<String, AriaDownloader> = hashMapOf()
        internal fun getDownloader(url: String): AriaDownloader? = downloaderMap[url]
    }
}
