package net.xzos.upgradeAll.server.app.manager.module

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.server.app.manager.AppManager
import net.xzos.upgradeAll.server.update.UpdateManager


class Updater internal constructor(private val appDatabaseId: Long) {

    val app = AppManager.getApp(appDatabaseId)
    private val engine = app.engine

    suspend fun getUpdateStatus(): Int {
        val installedVersioning = app.installedVersioning
        val isSuccessRenew = UpdateManager.renewApp(appDatabaseId)
        return if (isSuccessRenew) {
            //检查是否取得云端版本号
            if (installedVersioning != null || app.markProcessedVersionNumber != null) {
                // 检查是否获取本地版本号
                if (app.isLatest()) {
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

    suspend fun isSuccessRenew(): Boolean = engine.getReleaseInfo(0) != null

    // 获取最新版本号
    suspend fun getLatestVersioning(): String? = engine.getReleaseInfo(0)?.version_number

    internal fun nonBlockingDownloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false, context: Context? = null) =
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
