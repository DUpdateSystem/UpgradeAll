package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.CoreApi
import net.xzos.upgradeAll.server.app.engine.js.utils.JSLog
import net.xzos.upgradeAll.server.app.engine.js.utils.JSUtils
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

internal class JavaScriptCoreEngine(
        private val logObjectTag: Array<String>,
        private val URL: String?,
        private val jsCode: String?
) : CoreApi {
    private lateinit var cx: Context
    private lateinit var scope: Scriptable

    private val jsLog = JSLog(logObjectTag)
    internal val jsUtils = JSUtils(logObjectTag)

    // 加载 JavaScript 代码
    private fun executeVoidScript(): Boolean {
        if (jsCode == null) return false
        return try {
            cx.evaluateString(scope, jsCode, logObjectTag.toString(), 1, null)
            true
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, String.format("executeVoidScript: 脚本载入错误, ERROR_MESSAGE: %s", e.toString()))
            false
        }
    }

    private fun initRhino(): Boolean {
        // 初始化 rhino 对象
        cx = Context.enter()
        cx.optimizationLevel = -1
        scope = cx.initStandardObjects()
        // 载入 JavaScript 实例
        registerJavaMethods()
        val methodsSuccess = executeVoidScript()
        if (!methodsSuccess) return false
        ScriptableObject.putProperty(scope, "URL", URL)  // 初始化 URL
        return true
    }

    private fun exit() {
        Context.exit()
    }

    // 注册 Java 代码
    private fun registerJavaMethods() {
        // 爬虫库
        val rhinoJSUtils = Context.javaToJS(jsUtils, scope)
        ScriptableObject.putProperty(scope, "JSUtils", rhinoJSUtils)
        // Log
        val rhinoLogUtils = Context.javaToJS(jsLog, scope)
        ScriptableObject.putProperty(scope, "Log", rhinoLogUtils)
    }

    // 运行 JS 代码
    private fun runJS(functionName: String, args: Array<Any>): Any? {
        if (!initRhino()) return null
        val obj = scope.get(functionName, scope)
        val result = try {
            obj as Function
            obj.call(cx, scope, scope, args)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "runJS: 脚本执行错误, 函数名: $functionName, ERROR_MESSAGE: $e")
            null
        }
        exit()
        return result
    }

    override suspend fun getDefaultName(): String? {
        val result = runJS("getDefaultName", arrayOf())
        val defaultName = Context.toString(result) ?: return null
        Log.d(logObjectTag, TAG, "getDefaultName: defaultName: $defaultName")
        return defaultName
    }

    override suspend fun getReleaseNum(): Int {
        val result = runJS("getReleaseNum", arrayOf()) ?: return 0
        val releaseNum = Context.toNumber(result).toInt()
        Log.d(logObjectTag, TAG, "getReleaseNum: releaseNum: $releaseNum")
        return releaseNum
    }

    override suspend fun getVersioning(releaseNum: Int): String? {
        val args = arrayOf<Any>(releaseNum)
        val result = runJS("getVersioning", args) ?: runJS("getVersionNumber", args) ?: return null
        // TODO: 向下兼容两个主版本后移除，当前版本：0.1.0-alpha.3
        val versionNumber = Context.toString(result)
        Log.d(logObjectTag, TAG, "getVersioning: Versioning: $versionNumber")
        return versionNumber
    }

    override suspend fun getChangelog(releaseNum: Int): String? {
        val args = arrayOf<Any>(releaseNum)
        val result = runJS("getChangelog", args) ?: return null
        val changeLog = Context.toString(result)
        Log.d(logObjectTag, TAG, "getChangelog: Changelog: $changeLog")
        return changeLog
    }

    override suspend fun getReleaseDownload(releaseNum: Int): Map<String, String> {
        val args = arrayOf<Any>(releaseNum)
        val fileMap = mutableMapOf<String, String>()
        val result = runJS("getReleaseDownload", args) ?: return fileMap
        val fileJsonString = Context.toString(result)
        try {
            val returnMap = jsUtils.mapOfJsonObject(JSONObject(fileJsonString))
            for (key in returnMap.keys) {
                val keyString = key as String
                fileMap[keyString] = returnMap[key] as String
            }
        } catch (e: JSONException) {
            Log.e(logObjectTag, TAG, "getReleaseDownload: 返回值不符合 JsonObject 规范, fileJsonString : $fileJsonString")
        } catch (e: NullPointerException) {
            Log.e(logObjectTag, TAG, "getReleaseDownload: 返回值为 NULL, fileJsonString : $fileJsonString")
        }
        Log.d(logObjectTag, TAG, "getReleaseDownload: fileJson: $fileMap")
        return fileMap
    }

    override suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): String? {
        val result = runJS("downloadReleaseFile", arrayOf(downloadIndex.first, downloadIndex.second))
        val filePath: String? = if (result != null) {
            Context.toString(result)
        } else {
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
            if (fileName != null && downloadUrl != null)
                jsUtils.downloadFile(fileName, downloadUrl)
            else
                null
        }
        Log.d(logObjectTag, TAG, "downloadReleaseFile: filePath: $filePath")
        return filePath
    }

    companion object {
        private const val TAG = "JavaScriptCoreEngine"
        private val Log = ServerContainer.Log
    }
}
