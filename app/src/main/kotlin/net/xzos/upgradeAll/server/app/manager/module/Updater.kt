package net.xzos.upgradeAll.server.app.manager.module

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine


class Updater internal constructor(private val engine: JavaScriptEngine) {

    suspend fun isSuccessRenew(): Boolean = engine.getVersionNumber(0) != null

    // 获取最新版本号
    suspend fun getLatestVersioning(): String? = engine.getVersionNumber(0)

    internal fun nonBlockingDownloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false, context: Context? = null) =
            GlobalScope.launch {
                if (context != null)
                    launch(Dispatchers.Main) { Toast.makeText(context, R.string.ready_to_download, Toast.LENGTH_LONG).show() }
                downloadReleaseFile(fileIndex, externalDownloader)
            }

    // 使用内置下载器下载文件
    private suspend fun downloadReleaseFile(fileIndex: Pair<Int, Int>, externalDownloader: Boolean = false): String? =
            if (!externalDownloader) engine.downloadReleaseFile(fileIndex)
            else engine.downloadFile(fileIndex, externalDownloader = externalDownloader)
}