package net.xzos.upgradeall.core.filetasker

import android.content.Context
import net.xzos.upgradeall.core.downloader.*
import net.xzos.upgradeall.core.installer.ApkInstaller
import net.xzos.upgradeall.core.installer.isApkFile
import net.xzos.upgradeall.core.module.app.version_item.FileAsset
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.openInFileManager

class FileTasker internal constructor(
    appId: Map<String, String>, internal val fileAsset: FileAsset,
    downloadInfoList: List<DownloadInfoItem>? = null
) {
    val id: Int = getTaskerIndex()
    val name = fileAsset.name

    init {
        FileTaskerManager.addFileTasker(this)
    }

    /* 预下载器 */
    private var preDownload: PreDownload? = PreDownload(appId, fileAsset, downloadInfoList)

    /* 下载管理器 */
    var downloader: Downloader? = null

    suspend fun isInstallable(context: Context): Boolean =
        downloader?.downloadFile?.isApkFile(context)
            ?: false

    suspend fun install(
        failedInstallObserverFun: (Throwable) -> Unit,
        completeInstallFunc: () -> Unit,
        context: Context
    ) {
        if (isInstallable(context)) {
            downloader?.downloadFile?.getTmpFile(context)?.run {
                this.listFiles()?.filter { it.extension == "apk" }?.let {
                    when (it.size) {
                        0 -> return
                        1 -> {
                            ApkInstaller.install(it[0],
                                { e -> failedInstallObserverFun(e) },
                                { completeInstallFunc() }
                            )
                        }
                        else -> {
                            ApkInstaller.multipleInstall(
                                this,
                                { e -> failedInstallObserverFun(e) },
                                { completeInstallFunc() }
                            )
                        }
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
                downloader =
                    preDownload?.startDownload(taskStartedFun, overrideFailFun, *downloadOb)
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
        downloader?.downloadFile?.documentFile?.uri?.path?.run {
            openInFileManager(this, context)
        }
    }

    companion object {
        private val TASKER_INDEX = CoroutinesCount(0)
        private fun getTaskerIndex(): Int = TASKER_INDEX.up()

        fun FileAsset.getFileTasker(
            appId: Map<String, String>, downloadInfoList: List<DownloadInfoItem>? = null
        ) = FileTasker(appId, this, downloadInfoList)
    }
}