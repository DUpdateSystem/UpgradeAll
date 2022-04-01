package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntityManager
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadInfoItem
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.utils.URLReplace
import net.xzos.upgradeall.core.utils.oberver.Informer
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.file.DOWNLOAD_EXTRA_CACHE_DIR
import java.io.File

class DownloadTasker(
    val app: App, private val fileAsset: FileAsset
) : Informer {
    override val informerId: Int = Informer.getInformerId()
    var downloader: Downloader? = null
        private set
    private var _fileType: FileType? = null
    val fileType: FileType
        get() = _fileType ?: getFileType().apply {
            _fileType = this
        }

    var downloadInfoList: List<DownloadItem> = emptyList()

    private fun getDownloadSnapList() = downloader?.getStatusList() ?: listOf()

    private fun getFileTaskerSnap(status: DownloadTaskerStatus) =
        DownloadTaskerSnap(status, getDownloadSnapList())

    private suspend fun getDownloadInfo() {
        val hub = fileAsset.hub
        notifyChanged(getFileTaskerSnap(DownloadTaskerStatus.INFO_RENEW).msg(fileAsset.name))
        val urlReplaceUtil = URLReplace(ExtraHubEntityManager.getUrlReplace(hub.uuid))

        @Suppress("UNCHECKED_CAST")
        val appId = app.appId.filterValues { it != null } as Map<String, String>
        downloadInfoList = app.serverApi.getDownloadInfo(
            ApiRequestData(hub.uuid, hub.auth, appId), fileAsset.assetIndex
        ).map {
            it.copy(url = urlReplaceUtil.replaceURL(it.url))
        }
        if (downloadInfoList.isEmpty())
            notifyChanged(DownloadTaskerStatus.INFO_FAILED, DownloadInfoEmpty)
        else
            notifyChanged(DownloadTaskerStatus.INFO_COMPLETE)
    }

    suspend fun start(
        context: Context, externalDownload: Boolean,
    ) {
        getDownloadInfo()
        if (downloadInfoList.isEmpty()) {
            notifyChanged(getFileTaskerSnap(DownloadTaskerStatus.START_FAIL).error(DownloadInfoEmpty))
            return
        }
        if (externalDownload || PreferencesMap.enforce_use_external_downloader) {
            downloadInfoList.forEach {
                MiscellaneousUtils.accessByBrowser(it.url, context)
            }
            notifyChanged(getFileTaskerSnap(DownloadTaskerStatus.EXTERNAL_DOWNLOAD))
        } else {
            val downloader = setDownload(
                downloadInfoList.map { it.getDownloadInfoItem(fileAsset.name) },
                DOWNLOAD_EXTRA_CACHE_DIR
            )
            startDownloader(downloader)
            this.downloader = downloader
        }
    }

    private fun setDownload(infoList: List<DownloadInfoItem>, dir: File): Downloader {
        return Downloader(dir).apply {
            infoList.forEach {
                addTask(it)
            }
        }
    }

    private fun startDownloader(downloader: Downloader) {
        notifyChanged(getFileTaskerSnap(DownloadTaskerStatus.WAIT_START))
        downloader.start(
            {
                notifyChanged(getFileTaskerSnap(DownloadTaskerStatus.STARTED).msg(it.toString()))
            },
            {
                notifyChanged(getFileTaskerSnap(DownloadTaskerStatus.START_FAIL).error(it))
            }
        )
    }

    private fun notifyChanged(snap: DownloadTaskerSnap) {
        notifyChanged(snap.status, snap)
    }
}

fun DownloadTasker.defName() =
    downloader?.downloadFile?.getFileList()?.getOrNull(0)?.name ?: app.name