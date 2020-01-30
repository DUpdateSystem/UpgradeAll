package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.data.json.gson.JSReturnData
import net.xzos.upgradeAll.data.json.nongson.JSCache
import net.xzos.upgradeAll.data.json.nongson.ObjectTag
import net.xzos.upgradeAll.server.log.LogUtil

class JavaScriptEngine internal constructor(
        private val objectTag: ObjectTag,
        URL: String?,
        jsCode: String?,
        debugMode: Boolean = false
) {

    private val jsCache = JSCache(objectTag)
    private val javaScriptCoreEngine = JavaScriptCoreEngine(objectTag, URL, jsCode, jsCache)

    init {
        if (!debugMode) {
            Log.i(objectTag, TAG, "JavaScriptCoreEngine: jsCode: \n$jsCode")  // 只打印一次 JS 脚本
        }
        javaScriptCoreEngine.jsUtils.debugMode = debugMode

        jsCache.clearCache()  // 初始化类时，清除旧的缓存
    }

    suspend fun getDefaultName(): String? = javaScriptCoreEngine.getDefaultName()

    suspend fun getAppIconUrl(): String? = javaScriptCoreEngine.getAppIconUrl()

    /**
     * 返回包含版本信息的数据类列表
     */
    suspend fun getJsReturnData(): JSReturnData =
            jsCache.getJsReturnData()
                    ?: javaScriptCoreEngine.getReleaseInfo().also {
                        jsCache.cacheJsReturnData(it)
                    }

    suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): Boolean {
        val jsSuccessDownload = javaScriptCoreEngine.downloadReleaseFile(downloadIndex)
        return if (jsSuccessDownload)
            true
        else
            downloadFileByReleaseInfo(downloadIndex)
    }

    suspend fun downloadFileByReleaseInfo(downloadIndex: Pair<Int, Int>, externalDownloader: Boolean = false): Boolean {
        Log.e(objectTag, TAG, "downloadFile: 尝试直接下载")
        val assets = getJsReturnData().releaseInfoList[downloadIndex.first].assets
        val fileIndex = downloadIndex.second
        val fileName = assets[fileIndex].name
        val downloadUrl = assets[fileIndex].download_url
        javaScriptCoreEngine.jsUtils.downloadFile(fileName, downloadUrl, externalDownloader = externalDownloader)
        return true
    }

    companion object {
        private const val TAG = "JavaScriptEngine"
        private val Log = LogUtil
    }
}
