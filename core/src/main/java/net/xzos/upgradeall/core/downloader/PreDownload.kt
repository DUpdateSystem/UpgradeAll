package net.xzos.upgradeall.core.downloader

import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.core.module.network.GrpcApi
import net.xzos.upgradeall.core.module.network.toMap

class PreDownload(private val fileAsset: FileAsset) {
    private var cancelled = false

    fun cancel() {
        cancelled = true
    }

    suspend fun startDownload(
            taskStartedFun: (Int) -> Unit,
            taskStartFailedFun: () -> Unit,
            vararg downloadOb: DownloadOb,
    ): Downloader {
        return doDownload(fileAsset.name).apply {
            if (cancelled) throw RuntimeException(DOWNLOAD_CANCELLED)
            start(taskStartedFun, taskStartFailedFun, *downloadOb)
            pause()
        }
    }

    private suspend fun preDownload(): List<DownloadInfoItem> {
        val appId = fileAsset.app.appId
        val hubUuid = fileAsset.hub.uuid
        val defName = fileAsset.name
        val downloadResponse = GrpcApi.getDownloadInfo(hubUuid, appId, mapOf(), fileAsset.assetIndex)
        var list = downloadResponse?.listList?.map { downloadPackage ->
            val fileName = if (downloadPackage.name.isNotBlank())
                downloadPackage.name
            else {
                defName
            }
            DownloadInfoItem(
                    fileName, downloadPackage.url, downloadPackage.headersList?.toMap()
                    ?: mapOf(), downloadPackage.cookiesList?.toMap() ?: mapOf()
            )
        }
        if (list.isNullOrEmpty())
            list = listOf(DownloadInfoItem(defName, fileAsset.downloadUrl, mapOf(), mapOf()))
        return list
    }

    private suspend fun doDownload(name: String): Downloader {
        if (cancelled) throw RuntimeException(DOWNLOAD_CANCELLED)
        val list = preDownload()
        if (cancelled) throw RuntimeException(DOWNLOAD_CANCELLED)
        return Downloader().apply {
            list.forEach {
                addTask(it.name, it.url, it.headers, it.cookies)
            }
        }
    }

    companion object {
        private const val DOWNLOAD_CANCELLED = "DOWNLOAD_CANCELLED"
    }
}