package net.xzos.upgradeall.core.downloader.filetasker

import com.tonyodev.fetch2.Download
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.core.downloader.filedownloader.DownloadCanceledError
import net.xzos.upgradeall.core.downloader.filedownloader.DownloadFileError
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadInfoItem
import net.xzos.upgradeall.core.downloader.filedownloader.item.Downloader
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import net.xzos.upgradeall.core.utils.oberver.Informer
import java.io.File

open class FileTasker : Informer {

    lateinit var id: FileTaskerId
    override val informerId = Informer.getInformerId()

    val name get() = id.name

    protected val snapBuilder = FileTaskerSnapBuilder { downloader }
    var snap = snapBuilder.buildEmpty()

    /* 下载管理器 */
    var downloader: Downloader? = null
    val fileList get() = downloader?.downloadFile?.getFileList() ?: emptyList()

    protected fun notifySnapChanged(snap: FileTaskerSnap) {
        this.snap = snap
        notifyChanged(snap.status, snap)
        notifyChanged(snap)
    }

    protected open fun renewData() {}

    private val extraDownloadOb = DownloadOb({
        notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_START, it))
    }, {
        notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_RUNNING, it))
    }, {
        notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_STOP, it))
    }, {
        notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_COMPLETE, it))
    }, {
        notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_CANCEL, it))
    }, {
        notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_FAIL, it))
    })

    fun init(id: FileTaskerId) {
        this.id = id
        FileTaskerManager.addFileTasker(this)
    }

    fun setDownload(downloadInfoList: List<DownloadInfoItem>, downloadFile: File) {
        downloader = Downloader(downloadFile).apply {
            downloadInfoList.forEach {
                addTask(it.name, it.url, it.headers, it.cookies)
            }
        }
    }

    suspend fun startDownload(
        taskStartedFun: (Int) -> Unit,
        taskStartFailedFun: (Throwable) -> Unit,
    ) {
        val overrideStartedFun = fun(downloadId: Int) {
            taskStartedFun(downloadId)
            notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_QUEUE))
        }
        val overrideFailFun = fun(e: Throwable) {
            taskStartFailedFun(e)
            notifySnapChanged(
                snapBuilder.build(
                    FileTaskerStatus.DOWNLOAD_START_FAIL,
                    null, e
                )
            )
        }
        try {
            downloader?.start(overrideStartedFun, overrideFailFun, extraDownloadOb)
                ?: notifySnapChanged(snapBuilder.build(FileTaskerStatus.DOWNLOAD_NOT_SET))
        } catch (e: DownloadFileError) {
            taskStartFailedFun(e)
        } catch (e: DownloadCanceledError) {
            taskStartFailedFun(e)
        }
    }

    fun resume() = downloader?.resume()
    fun pause() = downloader?.pause()
    fun retry() = downloader?.retry()
    fun cancel() {
        downloader?.cancel()
        FileTaskerManager.removeFileTasker(this)
    }
}