package net.xzos.upgradeAll.server.app.engine.js

import kotlinx.coroutines.*
import net.xzos.upgradeAll.server.ServerContainer
import java.util.concurrent.Executors

class JavaScriptEngine internal constructor(
        private val logObjectTag: Array<String>,
        URL: String?,
        jsCode: String?,
        isDebug: Boolean = false
) {

    private val javaScriptCoreEngine: JavaScriptCoreEngine = JavaScriptCoreEngine(logObjectTag, URL, jsCode)

    private val executorCoroutineDispatcher: ExecutorCoroutineDispatcher = javascriptThreadList[javascriptThreadIndex]

    init {
        if (!isDebug) {
            Log.i(this.logObjectTag, TAG, String.format("JavaScriptCoreEngine: jsCode: \n%s", jsCode))  // 只打印一次 JS 脚本
        }
        javaScriptCoreEngine.jsUtils.isDebug = isDebug
        GlobalScope.launch(executorCoroutineDispatcher) { javaScriptCoreEngine.initRhino() }
    }


    fun getDefaultName(): String? {
        return runBlocking(executorCoroutineDispatcher) { javaScriptCoreEngine.getDefaultName() }
    }

    fun getReleaseNum(): Int {
        return runBlocking(executorCoroutineDispatcher) { javaScriptCoreEngine.getReleaseNum() }
    }

    fun getVersioning(releaseNum: Int): String? {
        return when {
            releaseNum >= 0 -> runBlocking(executorCoroutineDispatcher) { javaScriptCoreEngine.getVersioning(releaseNum) }
            else -> null
        }
    }

    fun getChangelog(releaseNum: Int): String? {
        return when {
            releaseNum >= 0 -> runBlocking(executorCoroutineDispatcher) { javaScriptCoreEngine.getChangelog(releaseNum) }
            else -> null
        }
    }

    fun getReleaseDownload(releaseNum: Int): Map<String, String> {
        return when {
            releaseNum >= 0 -> runBlocking(executorCoroutineDispatcher) { javaScriptCoreEngine.getReleaseDownload(releaseNum) }
            else -> mapOf()
        }
    }

    fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): String? {
        return runBlocking(executorCoroutineDispatcher) { javaScriptCoreEngine.downloadReleaseFile(downloadIndex) }
    }

    fun exit() {
        javaScriptCoreEngine.exit()
    }

    companion object {
        private const val TAG = "JavaScriptEngine"
        private val Log = ServerContainer.Log
        private var javascriptThreadList: MutableList<ExecutorCoroutineDispatcher> = mutableListOf()

        private const val threadNumber = 8
        // TODO: 用户自定义线程池大小
        private var javascriptThreadIndex: Int = 0
            get() {
                field++
                field %= threadNumber
                return field
            }

        init {
            for (i in 1..threadNumber) {
                javascriptThreadList.add(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
            }
        }
    }
}
