package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.downloader.DownloadInfoItem
import net.xzos.upgradeall.core.downloader.DownloadOb
import net.xzos.upgradeall.core.downloader.Downloader
import net.xzos.upgradeall.core.installer.ApkInstaller
import net.xzos.upgradeall.core.installer.isApkFile
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.network.GrpcApi
import net.xzos.upgradeall.core.module.network.toMap


/**
 * 文件数据列表
 * 用来提供下载{@link #download}、安装{@link #installable}{@link #install}功能
 */
class FileAsset(
        /* 文件数据名称，用来给用户看的 */
        val name: String,
        /* 默认下载链接 */
        internal val downloadUrl: String,
        internal val fileType: String,
        internal val assetIndex: Pair<Int, Int>,
        private val app: App,
        private val hub: Hub,
) {
    /* 下载管理器 */
    var downloader: Downloader? = null

    suspend fun download(
            taskStartedFun: (Int) -> Unit,
            taskStartFailedFun: () -> Unit,
            downloadOb: DownloadOb,
    ) {
        val appId = app.appId
        val hubUuid = hub.uuid
        val downloadResponse = GrpcApi.getDownloadInfo(hubUuid, appId, mapOf(), assetIndex)
        var list = downloadResponse?.listList?.map { downloadPackage ->
            val fileName = if (downloadPackage.name.isNotBlank())
                downloadPackage.name
            else {
                name
            }
            DownloadInfoItem(
                    fileName, downloadPackage.url, downloadPackage.headersList?.toMap()
                    ?: mapOf(), downloadPackage.cookiesList?.toMap() ?: mapOf()
            )
        }
        if (list.isNullOrEmpty())
            list = listOf(DownloadInfoItem(name, downloadUrl, mapOf(), mapOf()))
        downloader = Downloader(name, this).apply {
            for (downloadInfo in list) {
                addTask(
                        downloadInfo.name,
                        downloadInfo.url,
                        downloadInfo.headers,
                        downloadInfo.cookies
                )
            }
        }.also {
            it.start(taskStartedFun, taskStartFailedFun, downloadOb)
        }
    }

    val installable: Boolean
        get() = downloader?.downloadDir?.isApkFile() ?: false

    suspend fun install(failedInstallObserverFun: (Throwable) -> Unit, completeInstallFunc: () -> Unit) {
        if (installable) {
            downloader?.getFileList()?.run {
                when (this.size) {
                    0 -> return
                    1 -> {
                        ApkInstaller.install(this[0],
                                fun(e) { failedInstallObserverFun(e) },
                                fun(_) { completeInstallFunc() }
                        )
                    }
                    else -> {
                        ApkInstaller.multipleInstall(
                                downloader!!.downloadDir,
                                fun(e) { failedInstallObserverFun.call(e) },
                                fun(_) { completeInstallFunc.call() }
                        )
                    }
                }
            }
        }
    }

    companion object {
        class TmpFileAsset(
                /* 文件数据名称，用来给用户看的 */
                val name: String,
                /* 默认下载链接 */
                internal val downloadUrl: String,
                internal val fileType: String,
                internal val assetIndex: Pair<Int, Int>,
        )
    }
}