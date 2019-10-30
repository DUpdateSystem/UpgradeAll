package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.CoreApi

class JavaScriptEngine internal constructor(
        internal val logObjectTag: Array<String>,
        URL: String?,
        jsCode: String?,
        isDebug: Boolean = false
) : CoreApi {

    private val javaScriptCoreEngine: JavaScriptCoreEngine = JavaScriptCoreEngine(logObjectTag, URL, jsCode)

    init {
        if (!isDebug) {
            Log.i(this.logObjectTag, TAG, String.format("JavaScriptCoreEngine: jsCode: \n%s", jsCode))  // 只打印一次 JS 脚本
        }
        javaScriptCoreEngine.jsUtils.isDebug = isDebug
    }


    override suspend fun getDefaultName(): String? {
        return javaScriptCoreEngine.getDefaultName()
    }

    override suspend fun getAppIconUrl(): String? {
        return javaScriptCoreEngine.getAppIconUrl()
    }

    override suspend fun getReleaseNum(): Int {
        return javaScriptCoreEngine.getReleaseNum()
    }

    override suspend fun getVersioning(releaseNum: Int): String? {
        return when {
            releaseNum >= 0 -> javaScriptCoreEngine.getVersioning(releaseNum)
            else -> null
        }
    }

    override suspend fun getChangelog(releaseNum: Int): String? {
        return when {
            releaseNum >= 0 -> javaScriptCoreEngine.getChangelog(releaseNum)
            else -> null
        }
    }

    override suspend fun getReleaseDownload(releaseNum: Int): Map<String, String> {
        return when {
            releaseNum >= 0 -> javaScriptCoreEngine.getReleaseDownload(releaseNum)
            else -> mapOf()
        }
    }

    override suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): String? {
        return javaScriptCoreEngine.downloadReleaseFile(downloadIndex)
                ?: downloadFile(downloadIndex)

    }

    private suspend fun downloadFile(downloadIndex: Pair<Int, Int>): String? {
        Log.e(logObjectTag, TAG, "downloadReleaseFile: 尝试直接下载")
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
            javaScriptCoreEngine.jsUtils.downloadFile(fileName, downloadUrl)
        else
            null
    }

    companion object {
        private const val TAG = "JavaScriptEngine"
        private val Log = ServerContainer.Log
    }
}
