package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.data.json.gson.JSReturnData
import net.xzos.upgradeAll.data.json.nongson.JSCache
import net.xzos.upgradeAll.server.ServerContainer

class JavaScriptEngine internal constructor(
        internal val logObjectTag: Pair<String, String>,
        URL: String?,
        jsCode: String?,
        debugMode: Boolean = false
) {

    private val javaScriptCoreEngine = JavaScriptCoreEngine(logObjectTag, URL, jsCode)

    init {
        if (!debugMode) {
            Log.i(this.logObjectTag, TAG, String.format("JavaScriptCoreEngine: jsCode: \n%s", jsCode))  // 只打印一次 JS 脚本
        }
        javaScriptCoreEngine.jsUtils.debugMode = debugMode
        JSCache.clearCache(logObjectTag)
    }

    suspend fun getDefaultName(): String? = javaScriptCoreEngine.getDefaultName()

    suspend fun getAppIconUrl(): String? = javaScriptCoreEngine.getAppIconUrl()

    /**
     * 返回包含版本信息的数据类列表
     */
    private suspend fun getReleasesInfo(): List<JSReturnData.ReleaseInfoBean> =
            javaScriptCoreEngine.getReleaseInfo().releaseInfoList

    suspend fun getReleaseNum(): Int = getReleasesInfo().size

    suspend fun getReleaseInfo(releaseNum: Int): JSReturnData.ReleaseInfoBean? {
        val releasesInfo = getReleasesInfo()
        return if (releasesInfo.isNotEmpty() && releaseNum < releasesInfo.size)
            releasesInfo[releaseNum]
        else null
    }

    suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): Boolean {
        val jsSuccessDownload = javaScriptCoreEngine.downloadReleaseFile(downloadIndex)
        return if (jsSuccessDownload)
            true
        else
            downloadFileByReleaseInfo(downloadIndex)
    }

    suspend fun downloadFileByReleaseInfo(downloadIndex: Pair<Int, Int>, externalDownloader: Boolean = false): Boolean {
        Log.e(logObjectTag, TAG, "downloadFile: 尝试直接下载")
        val assets = getReleasesInfo()[downloadIndex.first].assets
        val fileIndex = downloadIndex.second
        val fileName = assets[fileIndex].name
        val downloadUrl = assets[fileIndex].download_url
        javaScriptCoreEngine.jsUtils.downloadFile(fileName, downloadUrl, externalDownloader = externalDownloader)
        return true
    }

    companion object {
        private const val TAG = "JavaScriptEngine"
        private val Log = ServerContainer.Log
    }
}
