package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.downloader.filedownloader.item.InputData
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.core.installer.Installer
import net.xzos.upgradeall.core.installer.getFileType
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.utils.oberver.Func
import net.xzos.upgradeall.core.utils.oberver.FuncNoArg
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.server.downloader.DownloadNotification
import net.xzos.upgradeall.server.downloader.DownloadNotificationManager

fun DownloadItem.getDownloadInfoItem(defName: String): InputData {
    return InputData(name ?: defName, url, headers ?: emptyMap(), cookies ?: emptyMap())
}

fun DownloadTasker.getFileList() = downloader?.getTaskList()?.map { it.file } ?: listOf()

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

suspend fun installFileTasker(
    context: Context, downloadTasker: DownloadTasker,
    notification: DownloadNotification? = DownloadNotificationManager.getNotification(downloadTasker)
) {
    downloadTasker.install(context, downloadTasker.fileType, {
        ToastUtil.showText(
            context, "${context.getString(R.string.install_failed)}: ${it.msg()}"
        )
    }, {
        notification?.cancelNotification()
        ToastUtil.showText(context, R.string.install_success)
    })
}