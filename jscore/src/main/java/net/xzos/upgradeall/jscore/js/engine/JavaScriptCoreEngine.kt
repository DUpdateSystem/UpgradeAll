package net.xzos.upgradeall.jscore.js.engine

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.data.json.gson.JSReturnData
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.log.LogUtilProxy
import net.xzos.upgradeall.jscore.utils.JSUtils
import net.xzos.upgradeall.log.Log
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

internal class JavaScriptCoreEngine(
        private val objectTag: ObjectTag,
        private val URL: String?,
        private val jsCode: String?
) : JavaScriptApi {
    private val jsLog = LogUtilProxy(objectTag, LogUtilProxy.JS_TAG)
    internal var jsUtils = JSUtils(objectTag)
    private lateinit var scope: ScriptableObject

    init {
        runBlocking { runJS(null, arrayOf()) }  // 空运行初始化基础 JavaScript 运行环境
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
            cx.evaluateString(scope, jsCode, this.toString(), 1, null)
            true
        } catch (e: Throwable) {
            Log.e(objectTag, TAG, "executeVoidScript: 脚本载入错误, ERROR_MESSAGE: $e")
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
            Log.e(objectTag, TAG, "runJS: 脚本执行错误, 函数名: $functionName, ERROR_MESSAGE: $e")
            null
        } finally {
            Context.exit()
        }
    }

    /**
     * 返回更新项默认名称
     */
    override suspend fun getDefaultName(): String? {
        val result = runJS("getDefaultName", arrayOf()) ?: return null
        val defaultName = Context.toString(result)
        Log.d(objectTag, TAG, "getDefaultName: defaultName: $defaultName")
        return defaultName
    }

    /**
     * 返回更新项默认图标 URL
     */
    override suspend fun getAppIconUrl(): String? {
        val result = runJS("getAppIconUrl", arrayOf())
                ?: return null
        val appIconUrl = Context.toString(result)
        Log.d(objectTag, TAG, "getAppIconUrl: appIconUrl: $appIconUrl")
        return appIconUrl
    }

    /**
     * 返回由 JavaScript 函数返回的固定 JSON 格式生成的版本信息数据类
     */
    override suspend fun getReleaseInfo(): JSReturnData {
        val result = runJS("getReleaseInfo", arrayOf())
                ?: return JSReturnData()
        val versionInfoJsonString: String = Context.toString(result)
        Log.d(objectTag, TAG, "getReleaseInfo: JSON 字符串: $versionInfoJsonString")
        return try {
            JSReturnData(
                    Gson().fromJson(versionInfoJsonString, Array<JSReturnData.ReleaseInfoBean>::class.java).also {
                        Log.d(objectTag, TAG, "getReleaseInfo: JSON 解析成功")
                    }.toList()
            )
        } catch (e: JsonSyntaxException) {
            Log.e(objectTag, TAG, "getReleaseInfo: JSON 解析失败, ERROR_MESSAGE: $e")
            JSReturnData()
        }
    }

    /**
     * 下载文件操作
     * 操作成功返回 true
     */
    override suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): Boolean =
            runJS("downloadReleaseFile", arrayOf(downloadIndex.first, downloadIndex.second)) != null

    companion object {
        private const val TAG = "JavaScriptCoreEngine"
    }
}

private interface JavaScriptApi {
    suspend fun getDefaultName(): String?
    suspend fun getAppIconUrl(): String?
    suspend fun getReleaseInfo(): JSReturnData?
    suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): Boolean
}