package net.xzos.upgradeall.wrapper.download

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.InputData
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.core.installer.Installer
import net.xzos.upgradeall.core.installer.getFileType
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.utils.oberver.Func
import net.xzos.upgradeall.core.utils.oberver.FuncNoArg
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.server.downloader.DownloadNotificationManager
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import java.io.IOException

fun DownloadItem.getDownloadInfoItem(defName: String): InputData {
    return InputData(name ?: defName, url, headers ?: emptyMap(), cookies ?: emptyMap())
}

fun DownloadTasker.getFileList() = rustDownloader?.getTaskList()?.map { it.file } ?: listOf()

internal suspend fun DownloadTasker.exportCompletedFiles(context: Context) {
    if (!PreferencesMap.auto_dump_download_file) return
    val saved = withContext(Dispatchers.IO) {
        try {
            val directory = DocumentFile.fromTreeUri(context, Uri.parse(PreferencesMap.user_download_path))
                ?: return@withContext false
            val files = getFileList()
            files.isNotEmpty() && files.all { source ->
                val mime = if (source.extension.equals("apk", ignoreCase = true))
                    "application/vnd.android.package-archive" else "application/octet-stream"
                val destination = directory.createFile(mime, source.name) ?: return@all false
                var complete = false
                try {
                    source.inputStream().use { input ->
                        val output = context.contentResolver.openOutputStream(destination.uri) ?: return@all false
                        output.use { input.copyTo(it) }
                    }
                    complete = true
                    true
                } finally {
                    if (!complete) destination.delete()
                }
            }
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
    if (!saved) ToastUtil.showText(context, R.string.save_failed)
}

fun DownloadTasker.getFileType() =
    getFileType(getFileList(), context)

suspend fun DownloadTasker.install(
    context: Context, fileType: FileType,
    failedInstallObserverFun: Func<Throwable>,
    completeInstallObserverFun: FuncNoArg
) {
    Installer.install(
        getFileList(), fileType, context, failedInstallObserverFun, completeInstallObserverFun
    )
}

suspend fun DownloadTasker.install(
    context: Context = MyApplication.context
) {
    install(context, fileType, {
        ToastUtil.showText(
            context, "${context.getString(R.string.install_failed)}: ${it.msg()}"
        )
    }, {
        DownloadNotificationManager.getNotification(this)?.cancelNotification()
        ToastUtil.showText(context, R.string.install_success)
        if (PreferencesMap.auto_delete_file) rustDownloader?.cancel()
    })
}