package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.data.json.nongson.JSCache
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.CoreApi

class JavaScriptEngine internal constructor(
        internal val logObjectTag: Array<String>,
        URL: String?,
        jsCode: String?,
        debugMode: Boolean = false
) : CoreApi {

    private val javaScriptCoreEngine = JavaScriptCoreEngine(logObjectTag, URL, jsCode)

    init {
        if (!debugMode) {
            Log.i(this.logObjectTag, TAG, String.format("JavaScriptCoreEngine: jsCode: \n%s", jsCode))  // 只打印一次 JS 脚本
        }
        javaScriptCoreEngine.jsUtils.debugMode = debugMode
        JSCache.clearCache(logObjectTag)
    }


    override suspend fun getDefaultName(): String? = javaScriptCoreEngine.getDefaultName()

    override suspend fun getAppIconUrl(): String? = javaScriptCoreEngine.getAppIconUrl()

    override suspend fun getReleaseNum(): Int = javaScriptCoreEngine.getReleaseNum()

    override suspend fun getVersioning(releaseNum: Int): String? = when {
        releaseNum >= 0 -> javaScriptCoreEngine.getVersioning(releaseNum)
        else -> null
    }

    override suspend fun getChangelog(releaseNum: Int): String? = when {
        releaseNum >= 0 -> javaScriptCoreEngine.getChangelog(releaseNum)
        else -> null
    }

    override suspend fun getReleaseDownload(releaseNum: Int): Map<String, String> = when {
        releaseNum >= 0 -> javaScriptCoreEngine.getReleaseDownload(releaseNum)
        else -> mapOf()
    }

    override suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): String? =
            javaScriptCoreEngine.downloadReleaseFile(downloadIndex)
                    ?: downloadFile(downloadIndex)

    internal suspend fun downloadFile(downloadIndex: Pair<Int, Int>, externalDownloader: Boolean = false): String? {
        Log.e(logObjectTag, TAG, "downloadFile: 尝试直接下载")
        val downloadReleaseMap = getReleaseDownload(downloadIndex.first)
        val fileIndex = downloadIndex.second
        val fileNameList = downloadReleaseMap.keys.toList()
        val fileName =
                if (fileIndex < fileNameList.size)
                    fileNameList[fileIndex]
                else
                    null
        val downloadUrl = downloadReleaseMap[fileName]
        return if (fileName != null && downloadUrl != null)
            javaScriptCoreEngine.jsUtils.downloadFile(fileName, downloadUrl, externalDownloader = externalDownloader)
        else
            null
    }

    companion object {
        private const val TAG = "JavaScriptEngine"
        private val Log = ServerContainer.Log
    }
}
