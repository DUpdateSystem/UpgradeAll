package net.xzos.upgradeall.wrapper.download

import android.content.Context
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntityManager
import net.xzos.upgradeall.core.downloader.filetasker.FileTasker
import net.xzos.upgradeall.core.downloader.filetasker.FileTaskerId
import net.xzos.upgradeall.core.installer.FileType
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.serverApi
import net.xzos.upgradeall.core.utils.URLReplaceUtil
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.file.DOWNLOAD_EXTRA_CACHE_DIR
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class FileTaskerWrapper(
    private val app: App, private val fileAsset: FileAsset
) : FileTasker() {
    init {
        val id = FileTaskerId(fileAsset.name, listOf(app, fileAsset))
        init(id)
    }

    private var _fileType: FileType? = null
    val fileType: FileType
        get() = _fileType ?: getFileType().apply {
            _fileType = this
        }


    var downloadInfoList: List<DownloadItem> = emptyList()

    override fun renewData() {
        _fileType = null
    }

    private suspend fun getDownloadInfo() {
        val hub = fileAsset.hub
        notifySnapChanged(
            snapBuilder.build(
                DownloadStatus.DOWNLOAD_INFO_RENEW,
                statusMsg = fileAsset.name
            )
        )
        val urlReplaceUtil = URLReplaceUtil(ExtraHubEntityManager.getUrlReplace(hub.uuid))
        downloadInfoList = serverApi.getDownloadInfo(
            hub.uuid, hub.auth, app.appId, fileAsset.assetIndex
        ).map {
            it.copy(url = urlReplaceUtil.replaceURL(it.url))
        }
        if (downloadInfoList.isEmpty())
            notifyChanged(DownloadStatus.DOWNLOAD_INFO_FAILED, DownloadInfoEmpty)
        else
            notifyChanged(DownloadStatus.DOWNLOAD_INFO_COMPLETE)
    }

    suspend fun start(
        context: Context, externalDownload: Boolean,
    ) {
        getDownloadInfo()
        if (downloadInfoList.isEmpty()) {
            notifySnapChanged(
                snapBuilder.build(
                    DownloadStatus.TASK_START_FAIL,
                    error = DownloadInfoEmpty
                )
            )
            return
        }
        if (externalDownload || PreferencesMap.enforce_use_external_downloader) {
            downloadInfoList.forEach {
                MiscellaneousUtils.accessByBrowser(it.url, context)
            }
            notifyChanged(DownloadStatus.EXTERNAL_DOWNLOAD)
        } else {
            setDownload(
                downloadInfoList.map { it.getDownloadInfoItem(fileAsset.name) },
                DOWNLOAD_EXTRA_CACHE_DIR
            )
            startDownloader()
        }
    }

    private suspend fun startDownloader() {
        notifyChanged(DownloadStatus.TASK_WAIT_START)
        startDownload(
            {
                notifySnapChanged(
                    snapBuilder.build(
                        DownloadStatus.TASK_STARTED,
                        statusMsg = it.toString()
                    )
                )
            },
            {
                notifySnapChanged(snapBuilder.build(DownloadStatus.TASK_START_FAIL, error = it))
            }
        )
    }

    /** Fetch 获取 Download 列表会导致 UI 阻塞 */
    fun getDownload(
        callback: (downloads: List<Download>) -> Unit,
        context: CoroutineContext = EmptyCoroutineContext,
    ) {
        GlobalScope.launch(context) {
            val list = downloader?.getDownloadList() ?: emptyList()
            callback(list)
        }
    }
}