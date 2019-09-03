package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.EngineApi
import org.json.JSONObject
import java.util.*

class JavaScriptEngine internal constructor(
        private var logObjectTag: Array<String>,
        URL: String,
        jsCode: String,
        enableLogJsCode: Boolean = true
) : EngineApi() {

    private var releaseNumCache = 0
    private val versionNumberCacheList = ArrayList<String?>()
    private val changeLogCacheList = ArrayList<String?>()
    private val releaseDownloadCacheList = ArrayList<JSONObject>()

    private val javaScriptCoreEngine: JavaScriptCoreEngine = JavaScriptCoreEngine(logObjectTag, URL, jsCode)

    init {
        if (enableLogJsCode) {
            Log.i(this.logObjectTag, TAG, String.format("JavaScriptCoreEngine: jsCode: \n%s", jsCode))  // 只打印一次 JS 脚本
        }
    }

    override fun refreshData() {
        releaseNumCache = releaseNum
        for (i in 0 until releaseNum) {
            versionNumberCacheList.add(getVersioning(i))
            changeLogCacheList.add(getChangelog(i))
            releaseDownloadCacheList.add(getReleaseDownload(i))
        }
    }

    override val defaultName: String?
        get() {
            return try {
                javaScriptCoreEngine.defaultName
            } catch (e: Throwable) {
                Log.e(logObjectTag, TAG, "defaultName: 脚本执行错误, ERROR_MESSAGE: $e")
                null
            }
        }
    override val releaseNum: Int
        get() {
            return if (releaseNumCache != 0) releaseNumCache
            else
                try {
                    javaScriptCoreEngine.releaseNum
                } catch (e: Throwable) {
                    Log.e(logObjectTag, TAG, "releaseNum: 脚本执行错误, ERROR_MESSAGE: $e")
                    0
                }
        }

    override fun getVersioning(releaseNum: Int): String? {
        return if (releaseNum < 0) null
        else if (versionNumberCacheList.isNotEmpty() && releaseNum < versionNumberCacheList.size)
            versionNumberCacheList[releaseNum]
        else if (versionNumberCacheList.isEmpty()) {
            try {
                javaScriptCoreEngine.getVersioning(releaseNum)
            } catch (e: Throwable) {
                Log.e(logObjectTag, TAG, "getVersioning: 脚本执行错误, ERROR_MESSAGE: $e")
                null
            }
        } else null
    }

    override fun getChangelog(releaseNum: Int): String? {
        return if (releaseNum < 0) null
        else if (changeLogCacheList.isNotEmpty() && releaseNum < changeLogCacheList.size)
            changeLogCacheList[releaseNum]
        else if (changeLogCacheList.isEmpty()) {
            try {
                javaScriptCoreEngine.getChangelog(releaseNum)
            } catch (e: Throwable) {
                Log.e(logObjectTag, TAG, "getChangelog: 脚本执行错误, ERROR_MESSAGE: $e")
                null
            }
        } else null
    }

    override fun getReleaseDownload(releaseNum: Int): JSONObject {
        return if (releaseNum < 0) JSONObject()
        else if (releaseDownloadCacheList.isNotEmpty() && releaseNum < releaseDownloadCacheList.size)
            releaseDownloadCacheList[releaseNum]
        else if (releaseDownloadCacheList.isEmpty()) {
            try {
                javaScriptCoreEngine.getReleaseDownload(releaseNum)
            } catch (e: Throwable) {
                Log.e(logObjectTag, TAG, "getReleaseDownload: 脚本执行错误, ERROR_MESSAGE: $e")
                JSONObject()
            }
        } else JSONObject()
    }

    companion object {
        private const val TAG = "JavaScriptEngine"
        private val Log = ServerContainer.Log
    }
}
