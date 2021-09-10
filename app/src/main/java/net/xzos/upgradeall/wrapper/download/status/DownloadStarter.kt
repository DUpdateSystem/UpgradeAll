package net.xzos.upgradeall.wrapper.download.status

import android.content.Context
import net.xzos.upgradeall.core.downloader.filedownloader.DownloadFetchError
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerId
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.serverApi
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.file.DOWNLOAD_EXTRA_CACHE_DIR
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.wrapper.download.getDownloadInfoItem

class DownloadStarter(
    private val app: App, private val fileAsset: FileAsset
) {
    val id = FileTaskerId(fileAsset.name, listOf(app, fileAsset))
    val downloadInformer = DownloadInformer(id)

    suspend fun start(
        context: Context, externalDownload: Boolean,
    ): FileTasker? {
        val hub = fileAsset.hub
        downloadInformer.notifyChanged(DownloadStatus.DOWNLOAD_WAIT_INFO, fileAsset.name)
        val downloadInfoList = serverApi.getDownloadInfo(
            hub.uuid, hub.auth, app.appId, fileAsset.assetIndex
        )
        if (downloadInfoList.isEmpty()) {
            downloadInformer.notifyChanged(DownloadStatus.TASK_START_FAIL, DownloadInfoEmpty)
            return null
        }
        return if (externalDownload || PreferencesMap.enforce_use_external_downloader) {
            downloadInfoList.forEach {
                MiscellaneousUtils.accessByBrowser(it.url, context)
            }
            downloadInformer.notifyChanged(DownloadStatus.EXTERNAL_DOWNLOAD)
            null
        } else {
            startDownloader(id, downloadInfoList, fileAsset.name)
        }
    }

    private suspend fun startDownloader(
        id: FileTaskerId, downloadInfoList: List<DownloadItem>, failbackName: String
    ): FileTasker {
        val fileTasker = FileTasker(
            id, downloadInfoList.map { it.getDownloadInfoItem(failbackName) },
            DOWNLOAD_EXTRA_CACHE_DIR
        )
        try {
            downloadInformer.notifyChanged(DownloadStatus.TASK_WAIT_START, fileTasker)
            fileTasker.startDownload(
                {
                    downloadInformer.register(fileTasker)
                    downloadInformer.notifyChanged(DownloadStatus.TASK_STARTED, it)
                },
                { downloadInformer.notifyChanged(DownloadStatus.TASK_START_FAIL, it) },
                downloadInformer.downloadOb
            )
        } catch (e: DownloadFetchError) {
            downloadInformer.notifyChanged(DownloadStatus.TASK_START_FAIL, e)
        }
        return fileTasker
    }
}