package net.xzos.upgradeAll.server.app.manager.module

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.data.json.gson.JSReturnData
import net.xzos.upgradeAll.utils.VersioningUtils


internal class Updater internal constructor(private val app: App) : UpdaterApi {

    private val engine = app.engine
    private var jsReturnData: JSReturnData? = null
        get() {
            return field ?: runBlocking {
                engine.getJsReturnData().also {
                    field = it
                }
            }
        }

    override suspend fun getUpdateStatus(): Int {
        val installedVersioning = app.installedVersioning
        val isSuccessRenew = isSuccessRenew()
        return if (isSuccessRenew) {
            //检查是否取得云端版本号
            if (installedVersioning != null || app.markProcessedVersionNumber != null) {
                // 检查是否获取本地版本号
                if (isLatest()) {
                    // 检查本地版本
                    APP_LATEST
                } else {
                    APP_OUTDATED
                }
            } else {
                APP_NO_LOCAL
            }
        } else {
            NETWORK_404
        }
    }

    private suspend fun isLatest(): Boolean {
        val latestVersion = getLatestVersioning()
        return VersioningUtils.compareVersionNumber(
                app.markProcessedVersionNumber ?: app.installedVersioning, latestVersion)
    }

    override suspend fun isSuccessRenew(): Boolean = jsReturnData!!.releaseInfoList.isNotEmpty()

    // 获取最新版本号
    override suspend fun getLatestVersioning(): String? {
        val releasesInfo = jsReturnData!!.releaseInfoList
        return if (releasesInfo.isNotEmpty())
            releasesInfo[0].version_number
        else null
    }

    override fun nonBlockingDownloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean, context: Context?) =
            GlobalScope.launch {
                if (context != null)
                    launch(Dispatchers.Main) { Toast.makeText(context, R.string.ready_to_download, Toast.LENGTH_LONG).show() }
                downloadReleaseFile(fileIndex, externalDownloader)
            }

    // 使用内置下载器下载文件
    private suspend fun downloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false): Boolean =
            if (!externalDownloader) engine.downloadReleaseFile(fileIndex)
            else engine.downloadFileByReleaseInfo(fileIndex, externalDownloader = externalDownloader)

    companion object {
        internal const val NETWORK_404 = 0
        internal const val APP_LATEST = 1
        internal const val APP_OUTDATED = 2
        internal const val APP_NO_LOCAL = 3

    }
}

private interface UpdaterApi {
    suspend fun isSuccessRenew(): Boolean
    suspend fun getUpdateStatus(): Int
    suspend fun getLatestVersioning(): String?
    fun nonBlockingDownloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false, context: Context? = null): Job
}
