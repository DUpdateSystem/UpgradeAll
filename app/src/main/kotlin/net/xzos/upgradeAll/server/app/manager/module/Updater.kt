package net.xzos.upgradeAll.server.app.manager.module

import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine

class Updater internal constructor(private val engine: JavaScriptEngine) {

    suspend fun isSuccessRenew(): Boolean {
        return engine.getVersioning(0) != null
    }

    // 获取最新版本号
    suspend fun getLatestVersioning(): String? {
        return engine.getVersioning(0)
    }

    // 获取最新更新日志
    suspend fun getLatestChangelog(): String? {
        return engine.getChangelog(0)
    }

    // 获取最新下载链接
    suspend fun getLatestReleaseDownload(): Map<String, String> {
        return engine.getReleaseDownload(0)
    }

    // 使用内置下载器下载文件
    suspend fun downloadReleaseFile(fileIndex: Pair<Int, Int>): String? {
        return engine.downloadReleaseFile(fileIndex)
    }
}