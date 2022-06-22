package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntityManager
import net.xzos.upgradeall.core.downloader.filedownloader.DownloaderManager
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskSnap
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.InputData
import net.xzos.upgradeall.core.downloader.filedownloader.newDownloader
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.utils.URLReplace
import net.xzos.upgradeall.core.utils.oberver.InformerNoTag
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.file.DOWNLOAD_EXTRA_CACHE_DIR
import java.io.File

class DownloadTasker(
    val app: App, private val fileAsset: FileAsset
) : InformerNoTag<DownloadTaskerSnap>() {
    var snap: DownloadTaskerSnap? = null
        private set

    var downloader: Downloader? = null
        private set
    private var _fileType: FileType? = null
    val fileType: FileType
        get() = _fileType ?: getFileType().apply {
            _fileType = this
        }

    var downloadInfoList: List<DownloadItem> = emptyList()

    private fun getDownloadSnapList() =
        downloader?.getTaskList()?.map { it.snap } ?: listOf()

    private fun getFileTaskerSnap(
        status: DownloadTaskerStatus? = null,
        snapList: List<TaskSnap> = getDownloadSnapList()
    ) = DownloadTaskerSnap(
        status ?: if (snapList.isNotEmpty()) DownloadTaskerStatus.IN_DOWNLOAD
        else throw RuntimeException("Error to build FileTaskerSnap(no status)"),
        snapList
    )

    private suspend fun getDownloadInfo() {
        val hub = fileAsset.hub
        changed(getFileTaskerSnap(DownloadTaskerStatus.INFO_RENEW).msg(fileAsset.name))
        val urlReplaceUtil = URLReplace(ExtraHubEntityManager.getUrlReplace(hub.uuid))
        val (appId, other) = hub.filterValidKey(app.appId)
        downloadInfoList = app.serverApi.getDownloadInfo(
            ApiRequestData(hub.uuid, hub.auth, appId, other), fileAsset.assetIndex
        ).map {
            it.copy(url = urlReplaceUtil.replaceURL(it.url))
        }
        if (downloadInfoList.isEmpty())
            changed(getFileTaskerSnap(DownloadTaskerStatus.INFO_FAILED).error(DownloadInfoEmpty))
        else
            changed(getFileTaskerSnap(DownloadTaskerStatus.INFO_COMPLETE))
    }

    suspend fun start(
        context: Context, externalDownload: Boolean,
    ) {
        getDownloadInfo()
        if (downloadInfoList.isEmpty()) {
            changed(getFileTaskerSnap(DownloadTaskerStatus.START_FAIL).error(DownloadInfoEmpty))
            return
        }
        if (externalDownload || PreferencesMap.enforce_use_external_downloader) {
            downloadInfoList.forEach {
                MiscellaneousUtils.accessByBrowser(it.url, context)
            }
            changed(getFileTaskerSnap(DownloadTaskerStatus.EXTERNAL_DOWNLOAD))
        } else {
            val downloader = setDownload(
                downloadInfoList.map { it.getDownloadInfoItem(fileAsset.name) },
                DOWNLOAD_EXTRA_CACHE_DIR
            )
            startDownloader(downloader)
            this.downloader = downloader
        }
    }

    private fun setDownload(dataList: List<InputData>, dir: File): Downloader {
        return DownloaderManager.newDownloader(dir).apply {
            dataList.forEach { addTask(it) }
        }.also { d ->
            d.observe {
                changed(getFileTaskerSnap(it.taskStatus()))
            }
        }
    }

    private suspend fun startDownloader(downloader: Downloader) {
        changed(getFileTaskerSnap(DownloadTaskerStatus.WAIT_START))
        downloader.start(
            { changed(getFileTaskerSnap(DownloadTaskerStatus.STARTED).msg("${app.name}$downloader")) },
            { changed(getFileTaskerSnap(DownloadTaskerStatus.START_FAIL).error(it)) }
        )
    }

    private fun changed(snap: DownloadTaskerSnap) {
        this.snap = snap
        notifyChanged(snap)
    }
}

fun DownloadTasker.defName() = downloader?.getTaskList()?.first()?.file?.name ?: app.name