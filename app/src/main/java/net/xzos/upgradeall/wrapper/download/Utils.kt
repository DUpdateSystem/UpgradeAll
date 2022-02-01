package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadInfoItem
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.core.installer.Installer
import net.xzos.upgradeall.core.installer.getFileType
import net.xzos.upgradeall.core.utils.log.msg
import net.xzos.upgradeall.core.utils.oberver.ObserverFun
import net.xzos.upgradeall.core.utils.oberver.ObserverFunNoArg
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.server.downloader.DownloadNotification
import net.xzos.upgradeall.server.downloader.DownloadNotificationManager

fun DownloadItem.getDownloadInfoItem(defName: String): DownloadInfoItem {
    return DownloadInfoItem(name ?: defName, url, headers ?: emptyMap(), cookies ?: emptyMap())
}

fun FileTasker.getFileType() =
    getFileType(fileList, context)

suspend fun FileTasker.install(
    context: Context, fileType: FileType,
    failedInstallObserverFun: ObserverFun<Throwable>,
    completeInstallObserverFun: ObserverFunNoArg
) {
    Installer.install(
        fileList, fileType, context, failedInstallObserverFun, completeInstallObserverFun
    )
}

suspend fun installFileTasker(
    context: Context, fileTasker: FileTaskerWrapper,
    notification: DownloadNotification? = DownloadNotificationManager.getNotification(fileTasker.id.toString())
) {
    fileTasker.install(context, fileTasker.fileType, {
        ToastUtil.showText(
            context, "${context.getString(R.string.install_failed)}: ${it.msg()}"
        )
    }, {
        notification?.cancelNotification()
        ToastUtil.showText(context, R.string.install_success)
    })
}