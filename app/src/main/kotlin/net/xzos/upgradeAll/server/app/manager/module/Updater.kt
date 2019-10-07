package net.xzos.upgradeAll.server.app.manager.module

import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.hub.HubManager
import org.litepal.LitePal
import org.litepal.extension.find


class Updater internal constructor(appDatabaseId: Long) {
    private val engine = newEngine(appDatabaseId)

    val isSuccessRenew: Boolean
        get() = engine.getVersioning(0) != null

    // 获取最新版本号
    val latestVersion: String?
        get() = engine.getVersioning(0)

    // 获取最新更新日志
    val latestChangelog: String?
        get() = engine.getChangelog(0)

    // 获取最新下载链接
    val latestReleaseDownload: Map<String, String>
        get() = engine.getReleaseDownload(0)

    // 使用内置下载器下载文件
    fun downloadReleaseFile(fileIndex: Pair<Int, Int>): String? {
        return engine.downloadReleaseFile(fileIndex)
    }

    internal fun engineExit() {
        engine.exit()
    }

    // 添加一个 更新检查器追踪子项
    private fun newEngine(databaseId: Long): JavaScriptEngine {
        val repoDatabase: RepoDatabase? = LitePal.find(databaseId)
        val apiUuid = repoDatabase?.api_uuid
        Log.d(LogObjectTag, TAG, "renewUpdateItem: uuid: $apiUuid")
        val url = repoDatabase?.url
        val apiName = repoDatabase?.api
        val logObjectTag = arrayOf(apiName.toString(), databaseId.toString())
        // 查找软件源数据库
        val jsCode = HubManager.getJsCode(apiUuid)
        return JavaScriptEngine(logObjectTag, url, jsCode)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "Updater"
        private val LogObjectTag = arrayOf("Core", TAG)
    }
}
