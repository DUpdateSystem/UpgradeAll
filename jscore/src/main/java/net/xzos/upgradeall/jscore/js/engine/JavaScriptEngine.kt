package net.xzos.upgradeall.jscore.js.engine

import net.xzos.upgradeall.data.json.gson.JSReturnData
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.data.json.nongson.JSCache
import net.xzos.upgradeall.log.Log


class JavaScriptEngine(
        private val objectTag: ObjectTag,
        URL: String?,
        jsCode: String?,
        debugMode: Boolean = false
) {

    private val javaScriptCoreEngine = JavaScriptCoreEngine(objectTag, URL, jsCode)

    init {
        if (!debugMode) {
            Log.i(objectTag, TAG, "JavaScriptCoreEngine: jsCode: \n$jsCode")  // 只打印一次 JS 脚本
        }
        javaScriptCoreEngine.jsUtils.debugMode = debugMode

        JSCache.clearCache(objectTag)  // 初始化类时，清除旧的缓存
    }

    suspend fun getDefaultName(): String? = javaScriptCoreEngine.getDefaultName()

    suspend fun getAppIconUrl(): String? = javaScriptCoreEngine.getAppIconUrl()

    /**
     * 返回包含版本信息的数据类列表
     */
    suspend fun getJsReturnData(): JSReturnData =
            JSCache.getJsReturnData(objectTag)
                    ?: javaScriptCoreEngine.getReleaseInfo().also {
                        JSCache.cacheJsReturnData(objectTag, it)
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
    }
}
