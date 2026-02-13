package net.xzos.upgradeall.wrapper.download

import android.content.Context
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntityManager
import net.xzos.upgradeall.core.downloader.filedownloader.item.TaskSnap
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version.AssetWrapper
import net.xzos.upgradeall.core.utils.URLReplace
import net.xzos.upgradeall.core.utils.oberver.InformerNoTag
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.getServerApi
import net.xzos.upgradeall.core.websdk.getterPort
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.getter.rpc.DownloaderHelper
import net.xzos.upgradeall.getter.rpc.RustDownloaderAdapter
import net.xzos.upgradeall.getter.rpc.toRustInputData
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.file.DOWNLOAD_EXTRA_CACHE_DIR
import net.xzos.upgradeall.websdk.data.json.DownloadItem
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DownloadTasker(
    val app: App, private val wrapper: AssetWrapper
) : InformerNoTag<DownloadTaskerSnap>() {
    val id: String by lazy { "${app.hashCode()}:${wrapper.hashCode()}" }
    private val assetIndex = wrapper.assetIndex
    val name = wrapper.asset.fileName
    var snap: DownloadTaskerSnap? = null
        private set

    var rustDownloader: RustDownloaderAdapter? = null
        private set
    private var _fileType: FileType? = null
    val fileType: FileType
        get() = _fileType ?: getFileType().apply {
            _fileType = this
        }

    var downloadInfoList: List<DownloadItem> = emptyList()

    private fun getDownloadSnapList(): List<TaskSnap> {
        return rustDownloader?.getTaskList()?.map { rustTask ->
            TaskSnap(
                status = rustTask.snap.status.toOldStatus(),
                downloadSize = rustTask.snap.downloadSize,
                totalSize = rustTask.snap.totalSize
            )
        } ?: listOf()
    }

    private fun net.xzos.upgradeall.getter.rpc.RustDownloadStatus.toOldStatus(): net.xzos.upgradeall.core.downloader.filedownloader.item.Status {
        return when (this) {
            net.xzos.upgradeall.getter.rpc.RustDownloadStatus.NONE -> net.xzos.upgradeall.core.downloader.filedownloader.item.Status.NONE
            net.xzos.upgradeall.getter.rpc.RustDownloadStatus.START -> net.xzos.upgradeall.core.downloader.filedownloader.item.Status.START
            net.xzos.upgradeall.getter.rpc.RustDownloadStatus.RUNNING -> net.xzos.upgradeall.core.downloader.filedownloader.item.Status.RUNNING
            net.xzos.upgradeall.getter.rpc.RustDownloadStatus.STOP -> net.xzos.upgradeall.core.downloader.filedownloader.item.Status.STOP
            net.xzos.upgradeall.getter.rpc.RustDownloadStatus.COMPLETE -> net.xzos.upgradeall.core.downloader.filedownloader.item.Status.COMPLETE
            net.xzos.upgradeall.getter.rpc.RustDownloadStatus.CANCEL -> net.xzos.upgradeall.core.downloader.filedownloader.item.Status.CANCEL
            net.xzos.upgradeall.getter.rpc.RustDownloadStatus.FAIL -> net.xzos.upgradeall.core.downloader.filedownloader.item.Status.FAIL
        }
    }

    private fun getFileTaskerSnap(
        status: DownloadTaskerStatus? = null,
        snapList: List<TaskSnap> = getDownloadSnapList()
    ) = DownloadTaskerSnap(
        status ?: if (snapList.isNotEmpty()) DownloadTaskerStatus.IN_DOWNLOAD
        else throw RuntimeException("Error to build FileTaskerSnap(no status)"),
        snapList
    )

    private suspend fun getDownloadInfo() {
        val hub = wrapper.hub
        changed(getFileTaskerSnap(DownloadTaskerStatus.INFO_RENEW).msg(name))
        val urlReplaceUtil = URLReplace(ExtraHubEntityManager.getUrlReplace(hub.uuid))
        val (appId, other) = hub.filterValidKey(app.appId)
        downloadInfoList = getServerApi().getDownloadInfo(
            SingleRequestData(HubData(hub.uuid, hub.auth), AppData(appId, other)),
            Pair(assetIndex[0], assetIndex[1])
        )?.map {
            it.copy(url = urlReplaceUtil.replaceURL(it.url))
        } ?: emptyList()
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
                MiscellaneousUtils.accessByBrowser(
                    it.url, context,
                    PreferencesMap.external_downloader_package_name
                )
            }
            changed(getFileTaskerSnap(DownloadTaskerStatus.EXTERNAL_DOWNLOAD))
        } else {
            rustDownloader = setRustDownload(
                downloadInfoList,
                DOWNLOAD_EXTRA_CACHE_DIR
            ).apply {
                startRustDownloader(this)
            }
        }
    }

    private fun setRustDownload(downloadList: List<DownloadItem>, dir: File): RustDownloaderAdapter {
        return DownloaderHelper.createRustDownloader(
            getterPort.getService(),
            dir,
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ).apply {
            downloadList.forEach { addTask(it.toRustInputData(name)) }
        }.also { d ->
            d.observe { status ->
                val snap = getFileTaskerSnap(status.taskStatus())
                // Attach error message if failed
                if (status == net.xzos.upgradeall.getter.rpc.RustDownloadStatus.FAIL) {
                    val errorMsg = d.getTaskList()
                        .mapNotNull { it.snap.error }
                        .firstOrNull()
                    errorMsg?.let { snap.msg(it) }
                }
                changed(snap)
            }
        }
    }

    private suspend fun startRustDownloader(downloader: RustDownloaderAdapter) {
        changed(getFileTaskerSnap(DownloadTaskerStatus.WAIT_START))
        downloader.start(
            { changed(getFileTaskerSnap(DownloadTaskerStatus.STARTED).msg("${app.name}")) },
            { changed(getFileTaskerSnap(DownloadTaskerStatus.START_FAIL).error(it)) }
        )
    }

    private fun changed(snap: DownloadTaskerSnap) {
        this.snap = snap
        notifyChanged(snap)
    }
}

fun DownloadTasker.defName() = rustDownloader?.getTaskList()?.first()?.file?.name ?: app.name