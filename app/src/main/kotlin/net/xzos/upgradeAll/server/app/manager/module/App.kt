package net.xzos.upgradeAll.server.app.manager.module

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.json.gson.VersionCheckerGson
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.utils.VersionChecker
import org.litepal.LitePal
import org.litepal.extension.find

data class App(private val appDatabaseId: Long) {
    private lateinit var logObjectTag: Array<String>
    internal val engine = newEngine(appDatabaseId)

    suspend fun isLatest(): Boolean {
        val latestVersion = Updater(engine).getLatestVersion()
        val installedVersion = installedVersioning
        return VersionChecker.compareVersionNumber(installedVersion, latestVersion)
    }

    // 获取最新版本号
    val latestVersioning: String?
        get() {
            return runBlocking { Updater(engine).getLatestVersion() }
        }

    // 获取已安装版本号
    val installedVersioning: String?
        get() {
            return versionChecker?.version
        }

    // 获取数据库 VersionCheckerGson 数据
    private val versionChecker: VersionChecker?
        get() {
            val repoDatabase: RepoDatabase? = LitePal.find(appDatabaseId)
            val versionChecker = repoDatabase?.versionCheckerGson
            return if (versionChecker is VersionCheckerGson)
                VersionChecker(versionChecker)
            else null
        }

    // 添加一个 更新检查器追踪子项
    private fun newEngine(databaseId: Long): JavaScriptEngine {
        val repoDatabase: RepoDatabase? = LitePal.find(databaseId)
        val apiUuid = repoDatabase?.api_uuid
        val url = repoDatabase?.url
        val apiName = repoDatabase?.api
        logObjectTag = arrayOf(apiName.toString(), databaseId.toString())
        // 查找软件源数据库
        val jsCode = HubManager.getJsCode(apiUuid)
        Log.d(logObjectTag, TAG, "renewUpdateItem: uuid: $apiUuid")
        return JavaScriptEngine(logObjectTag, url, jsCode)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "App"
    }
}
