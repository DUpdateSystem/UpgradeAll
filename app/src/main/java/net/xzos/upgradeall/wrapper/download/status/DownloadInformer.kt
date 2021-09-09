package net.xzos.upgradeall.wrapper.download.status

import android.content.Context
import net.xzos.upgradeall.core.downloader.filedownloader.DownloadFetchError
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerId
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.serverApi
import net.xzos.upgradeall.core.utils.oberver.Informer
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.DOWNLOAD_EXTRA_CACHE_DIR
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.wrapper.download.getDownloadInfoItem


class DownloadInformer : Informer {

    override val informerId = Informer.getInformerId()

    private lateinit var fileTasker: FileTasker

    val id: String get() = fileTasker.id.toString()

    private val downloadOb by lazy {
        DownloadOb(
            startFunc = { notifyChanged(DownloadStatus.DOWNLOAD_START, it) },
            runningFunc = { notifyChanged(DownloadStatus.DOWNLOADING, it) },
            stopFunc = { notifyChanged(DownloadStatus.DOWNLOAD_STOP, it) },
            completeFunc = { notifyChanged(DownloadStatus.DOWNLOAD_COMPLETE, it) },
            cancelFunc = { notifyChanged(DownloadStatus.DOWNLOAD_CANCEL, it) },
            failFunc = { notifyChanged(DownloadStatus.DOWNLOAD_FAIL, it) },
        )
    }

    fun unregister() {
        fileTasker.downloader.unregister(downloadOb)
        DownloadInformerManager.remove(this)
    }

    suspend fun start(
        context: Context, externalDownload: Boolean,
        app: App, fileAsset: FileAsset
    ): FileTasker? {
        val hub = fileAsset.hub
        notifyChanged(DownloadStatus.DOWNLOAD_WAIT_INFO, fileAsset.name)
        val downloadInfoList = serverApi.getDownloadInfo(
            hub.uuid, hub.auth, app.appId, fileAsset.assetIndex
        )
        if (downloadInfoList.isEmpty()) {
            notifyChanged(DownloadStatus.TASK_START_FAIL, DownloadInfoEmpty)
            return null
        }
        return if (externalDownload || PreferencesMap.enforce_use_external_downloader) {
            downloadInfoList.forEach {
                MiscellaneousUtils.accessByBrowser(it.url, context)
            }
            notifyChanged(DownloadStatus.EXTERNAL_DOWNLOAD)
            null
        } else {
            startDownloader(
                FileTaskerId(fileAsset.name, listOf(app, fileAsset)),
                downloadInfoList, fileAsset.name
            )
        }
    }

    private suspend fun startDownloader(
        id: FileTaskerId, downloadInfoList: List<DownloadItem>, failbackName: String
    ): FileTasker {
        fileTasker = FileTasker(
            id, downloadInfoList.map { it.getDownloadInfoItem(failbackName) },
            DOWNLOAD_EXTRA_CACHE_DIR
        )
        try {
            notifyChanged(DownloadStatus.TASK_WAIT_START, fileTasker)
            fileTasker.startDownload(
                { notifyChanged(DownloadStatus.DOWNLOAD_START) },
                {
                    notifyChanged(DownloadStatus.TASK_START_FAIL, it)
                }, downloadOb
            )
        } catch (e: DownloadFetchError) {
            notifyChanged(DownloadStatus.TASK_START_FAIL, e)
        }
        return fileTasker
    }
}