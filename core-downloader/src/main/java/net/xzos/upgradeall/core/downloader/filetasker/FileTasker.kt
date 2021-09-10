package net.xzos.upgradeall.core.downloader.filetasker

import net.xzos.upgradeall.core.downloader.filedownloader.DownloadCanceledError
import net.xzos.upgradeall.core.downloader.filedownloader.DownloadFileError
import net.xzos.upgradeall.core.downloader.filedownloader.item.DownloadInfoItem
import net.xzos.upgradeall.core.downloader.filedownloader.item.PreDownload
import net.xzos.upgradeall.core.downloader.filedownloader.observe.DownloadOb
import java.io.File

class FileTasker(
    val id: FileTaskerId, downloadInfoList: List<DownloadInfoItem>, downloadFile: File
) {

    init {
        FileTaskerManager.addFileTasker(this)
    }

    val name = id.name

    /* 下载管理器 */
    val downloader = PreDownload.setDownload(downloadFile, downloadInfoList)
    val fileList by lazy { downloader.downloadFile.getFileList() ?: emptyList() }

    suspend fun startDownload(
        taskStartedFun: (Int) -> Unit,
        taskStartFailedFun: (Throwable) -> Unit,
        vararg downloadOb: DownloadOb,
    ) {
        val overrideFailFun = fun(e: Throwable) {
            taskStartFailedFun(e)
            FileTaskerManager.removeFileTasker(this)
        }
        try {
            downloader.start(taskStartedFun, overrideFailFun, *downloadOb)
        } catch (e: DownloadFileError) {
            taskStartFailedFun(e)
        } catch (e: DownloadCanceledError) {
            taskStartFailedFun(e)
        }
    }

    fun resume() = downloader.resume()
    fun pause() = downloader.pause()
    fun retry() = downloader.retry()
    fun cancel() {
        downloader.cancel()
        FileTaskerManager.removeFileTasker(this)
    }
}