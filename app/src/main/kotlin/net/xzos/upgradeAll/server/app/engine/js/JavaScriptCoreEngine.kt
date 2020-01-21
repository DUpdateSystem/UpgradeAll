package net.xzos.upgradeAll.server.app.engine.js

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import net.xzos.upgradeAll.data.json.gson.JSReturnData
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.CoreApi
import net.xzos.upgradeAll.server.app.engine.js.utils.JSLog
import net.xzos.upgradeAll.server.app.engine.js.utils.JSUtils
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

internal class JavaScriptCoreEngine(
        private val logObjectTag: Pair<String, String>,
        private val URL: String?,
        private val jsCode: String?
) : CoreApi {

    private val jsLog = JSLog(logObjectTag)
    internal lateinit var jsUtils: JSUtils
    private lateinit var scope: ScriptableObject

    init {
        runJS(null, arrayOf())  // 空运行初始化基础 JavaScript 运行环境
    }

    private fun initRhino(cx: Context, scope: ScriptableObject) {
        // 初始化 rhino 对象
        cx.optimizationLevel = -1
        // 载入 JavaScript 实例
        registerJavaMethods(cx, scope)
        val methodsSuccess = executeVoidScript(cx, scope)
        if (methodsSuccess)
            ScriptableObject.putProperty(scope, "URL", URL)  // 初始化 URL
    }

    // 注册 Java 代码
    private fun registerJavaMethods(cx: Context, scope: ScriptableObject) {
        // 爬虫库
        if (!::jsUtils.isInitialized)
            jsUtils = JSUtils(logObjectTag, scope)
        val rhinoJSUtils = Context.javaToJS(jsUtils.get(cx), scope)
        ScriptableObject.putProperty(scope, "JSUtils", rhinoJSUtils)
        // Log
        val rhinoLogUtils = Context.javaToJS(jsLog, scope)
        ScriptableObject.putProperty(scope, "Log", rhinoLogUtils)
    }

    // 加载 JavaScript 代码
    private fun executeVoidScript(cx: Context, scope: ScriptableObject): Boolean {
        if (jsCode == null) return false
        return try {
            cx.evaluateString(scope, jsCode, logObjectTag.toString(), 1, null)
            true
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, String.format("executeVoidScript: 脚本载入错误, ERROR_MESSAGE: %s", e.toString()))
            false
        }
    }

    // 运行 JS 代码
    private fun runJS(functionName: String?, args: Array<Any>): Any? {
        val cx = Context.enter()
        val scope = cx.initStandardObjects().also {
            initRhino(cx, it)
            scope = it
        }
        return try {
            if (functionName != null) {
                (scope.get(functionName, scope) as Function).call(cx, scope, scope, args)
            } else null
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "runJS: 脚本执行错误, 函数名: $functionName, ERROR_MESSAGE: $e")
            null
        } finally {
            Context.exit()
        }
    }

    override suspend fun getDefaultName(): String? {
        val result = runJS("getDefaultName", arrayOf()) ?: return null
        val defaultName = Context.toString(result)
        Log.d(logObjectTag, TAG, "getDefaultName: defaultName: $defaultName")
        return defaultName
    }

    override suspend fun getAppIconUrl(): String? {
        val result = runJS("getAppIconUrl", arrayOf())
                ?: return null
        val appIconUrl = Context.toString(result)
        Log.d(logObjectTag, TAG, "getAppIconUrl: appIconUrl: $appIconUrl")
        return appIconUrl
    }

    override suspend fun getReleaseNum(): Int {
        val result = getReleaseInfo()?.releaseInfoList?.size
                ?: runJS("getReleaseNum", arrayOf())
                ?: return 0
        val releaseNum = Context.toNumber(result).toInt()
        Log.d(logObjectTag, TAG, "getReleaseNum: releaseNum: $releaseNum")
        return releaseNum
    }

    override suspend fun getVersionNumber(releaseNum: Int): String? {
        val result = getReleaseInfo()?.releaseInfoList?.get(releaseNum)?.version_number
                ?: runJS("getVersionNumber", arrayOf(releaseNum))
                ?: return null
        // TODO: 向下兼容两个主版本后移除，当前版本：0.1.0-alpha.3
        val versionNumber = Context.toString(result)
        Log.d(logObjectTag, TAG, "getVersionNumber: getVersionNumber: $versionNumber")
        return versionNumber
    }

    override suspend fun getChangelog(releaseNum: Int): String? {
        val result = getReleaseInfo()?.releaseInfoList?.get(releaseNum)?.change_log
                ?: runJS("getChangelog", arrayOf(releaseNum))
                ?: return null
        val changeLog = Context.toString(result)
        Log.d(logObjectTag, TAG, "getChangelog: Changelog: $changeLog")
        return changeLog
    }

    override suspend fun getReleaseDownload(releaseNum: Int): Map<String, String> {
        val fileMap = mutableMapOf<String, String>()
        getReleaseInfo()?.releaseInfoList?.get(releaseNum)?.assets?.also { assets ->
            for (asset in assets)
                fileMap[asset.name] = asset.download_url
        } ?: kotlin.run {
            val result = runJS("getReleaseDownload", arrayOf(releaseNum))
                    ?: return fileMap
            val fileJsonString = Context.toString(result)
            try {
                val returnMap = jsUtils.mapOfJsonObject(fileJsonString)
                for (key in returnMap.keys) {
                    val keyString = key as String
                    fileMap[keyString] = returnMap[key] as String
                }
            } catch (e: NullPointerException) {
                Log.e(logObjectTag, TAG, "getReleaseDownload: 返回值为 NULL, fileJsonString : $fileJsonString")
            }
            Log.d(logObjectTag, TAG, "getReleaseDownload: fileJson: $fileMap")
        }
        return fileMap
    }

    override suspend fun getReleaseInfo(): JSReturnData? {
        val result = runJS("getReleaseInfo", arrayOf())
                ?: return null
        val versionInfoJsonString: String = Context.toString(result)
        return try {
            Gson().fromJson(versionInfoJsonString, JSReturnData::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    override suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): String? {
        val result = runJS("downloadReleaseFile", arrayOf(downloadIndex.first, downloadIndex.second))
                ?: return null
        val filePath: String = Context.toString(result)
        Log.d(logObjectTag, TAG, "downloadReleaseFile: filePath: $filePath")
        return filePath
    }

    companion object {
        private const val TAG = "JavaScriptCoreEngine"
        private val Log = ServerContainer.Log
    }
}