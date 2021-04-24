package net.xzos.upgradeall.core.filetasker

import android.content.Context
import net.xzos.upgradeall.core.downloader.*
import net.xzos.upgradeall.core.installer.ApkInstaller
import net.xzos.upgradeall.core.module.app.FileAsset
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.openInFileManager

class FileTasker internal constructor(
        internal val fileAsset: FileAsset,
        downloadInfoList: List<DownloadInfoItem>? = null
) {
    val id: Int = getTaskerIndex()
    val name = fileAsset.name

    init {
        FileTaskerManager.addFileTasker(this)
    }

    /* 预下载器 */
    private var preDownload: PreDownload? = PreDownload(fileAsset, downloadInfoList)

    /* 下载管理器 */
    var downloader: Downloader? = null

    val installable: Boolean
        get() = downloader?.downloadDir?.isApkFile() ?: false

    suspend fun install(failedInstallObserverFun: (Throwable) -> Unit, completeInstallFunc: () -> Unit) {
        if (installable) {
            downloader?.getFileList()?.run {
                when (this.size) {
                    0 -> return
                    1 -> {
                        ApkInstaller.install(this[0],
                                { e -> failedInstallObserverFun(e) },
                                { completeInstallFunc() }
                        )
                    }
                    else -> {
                        ApkInstaller.multipleInstall(
                                downloader!!.downloadDir,
                                { e -> failedInstallObserverFun(e) },
                                { completeInstallFunc() }
                        )
                    }
                }
            }
        }
    }

    suspend fun startDownload(
            taskStartedFun: (Int) -> Unit,
            taskStartFailedFun: (Throwable) -> Unit,
            vararg downloadOb: DownloadOb,
    ) {
        if (downloader == null) {
            val overrideFailFun = fun(e: Throwable) {
                taskStartFailedFun(e)
                FileTaskerManager.removeFileTasker(this)
            }
            try {
                downloader = preDownload?.startDownload(taskStartedFun, overrideFailFun, *downloadOb)
            } catch (e: DownloadFileError) {
                taskStartFailedFun(e)
            } catch (e: DownloadCanceledError) {
                taskStartFailedFun(e)
            }
            preDownload = null
        }
    }

    fun resume() = downloader?.resume()
    fun pause() = downloader?.pause()
    fun retry() = downloader?.retry()
    fun cancel() {
        downloader?.cancel() ?: preDownload?.cancel()
        FileTaskerManager.removeFileTasker(this)
    }

    fun openDownloadDir(context: Context) {
        downloader?.downloadDir?.uri?.path?.run {
            openInFileManager(this, context)
        }
    }

    companion object {
        private val TASKER_INDEX = CoroutinesCount(0)
        private fun getTaskerIndex(): Int = TASKER_INDEX.up()

        fun FileAsset.getFileTasker(downloadInfoList: List<DownloadInfoItem>? = null) = FileTasker(this, downloadInfoList)
    }
}