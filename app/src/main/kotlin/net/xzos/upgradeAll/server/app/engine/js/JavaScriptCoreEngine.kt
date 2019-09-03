package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.json.cache.JSCacheData
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

internal class JavaScriptCoreEngine(private val logObjectTag: Array<String>, private val URL: String, private val jsCode: String?) : CoreApi {

    private val jsUtils: JSUtils = JSUtils(this.logObjectTag)

    // 加载 JavaScript 代码
    private fun executeVoidScript(): Boolean {
        if (jsCode == null) return false
        return try {
            cx.evaluateString(scope, jsCode, null, 1, null)
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

    private fun closeRhino() {
        Context.exit()
    }

    // 注册 Java 代码
    private fun registerJavaMethods() {
        // 爬虫库
        val rhinoJSUtils = Context.javaToJS(jsUtils, scope)
        ScriptableObject.putProperty(scope, "JSUtils", rhinoJSUtils)
        // Log
        val rhinoLogUtils = Context.javaToJS(JSLog(logObjectTag), scope)
        ScriptableObject.putProperty(scope, "Log", rhinoLogUtils)
    }

    // 运行 JS 代码
    private fun runJS(functionName: String, args: Array<Any>): Any? {
        if (!initRhino()) return null // 初始化 J2V8
        val obj = scope.get(functionName, scope)
        val result =
                if (obj is Function) {
                    obj.call(cx, scope, scope, args)
                } else
                    null
        closeRhino() // 销毁 J2V8 对象
        return result
    }

    override val defaultName: String?
        get() {
            val result = runJS("getDefaultName", arrayOf())
            val defaultName =
                    if (result != null) Context.toString(result)
                    else null
            Log.d(logObjectTag, TAG, "defaultName: defaultName: $defaultName")
            return defaultName
        }

    override val releaseNum: Int
        get() {
            val result = runJS("getReleaseNum", arrayOf())
            val releaseNum =
                    if (result != null) Context.toNumber(result).toInt()
                    else 0
            Log.d(logObjectTag, TAG, "releaseNum: releaseNum: $releaseNum")
            return releaseNum
        }

    override fun getVersioning(releaseNum: Int): String? {
        val args = arrayOf<Any>(releaseNum)
        val result = runJS("getVersioning", args) ?: runJS("getVersionNumber", args)
        // TODO: 向下兼容两个主版本后移除，当前版本：0.1.0-alpha.3
        val versionNumber =
                if (result != null) Context.toString(result)
                else null
        Log.d(logObjectTag, TAG, "getVersioning: Versioning: $versionNumber")
        return versionNumber
    }

    override fun getChangelog(releaseNum: Int): String? {
        val args = arrayOf<Any>(releaseNum)
        val result = runJS("getChangelog", args)
        val changeLog =
                if (result != null) Context.toString(result)
                else null
        Log.d(logObjectTag, TAG, "getChangelog: Changelog: $changeLog")
        return changeLog
    }

    override fun getReleaseDownload(releaseNum: Int): JSONObject {
        val args = arrayOf<Any>(releaseNum)
        val result = runJS("getReleaseDownload", args)
        val fileJsonString =
                if (result != null) Context.toString(result)
                else JSONObject().toString()
        var fileJson = JSONObject()
        try {
            fileJson = JSONObject(fileJsonString)
        } catch (e: JSONException) {
            Log.e(logObjectTag, TAG, "getReleaseDownload: 返回值不符合 JsonObject 规范, fileJsonString : $fileJsonString")
        } catch (e: NullPointerException) {
            Log.e(logObjectTag, TAG, "getReleaseDownload: 返回值为 NULL, fileJsonString : $fileJsonString")
        }

        Log.d(logObjectTag, TAG, "getReleaseDownload: fileJson: $fileJson")
        return fileJson
    }

    companion object {
        private const val TAG = "JavaScriptCoreEngine"
        private val Log = ServerContainer.Log
        private lateinit var cx: Context
        private lateinit var scope: Scriptable
    }
}
