package net.xzos.upgradeAll.server.app.manager.module

import net.xzos.upgradeAll.data.database.litepal.RepoDatabase
import net.xzos.upgradeAll.data.database.manager.HubDatabaseManager
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.utils.VersioningUtils
import org.litepal.LitePal
import org.litepal.extension.find

data class App(private val appDatabaseId: Long) {
    private lateinit var logObjectTag: Array<String>
    internal val engine = newEngine(appDatabaseId)

    suspend fun isLatest(): Boolean {
        val latestVersion = Updater(engine).getLatestVersioning()
        val installedVersion = installedVersioning
        return VersioningUtils.compareVersionNumber(installedVersion, latestVersion)
    }

    // 获取已安装版本号
    val installedVersioning: String?
        get() {
            return versioningUtils?.version
        }

    // 获取数据库 VersionCheckerGson 数据
    private val versioningUtils: VersioningUtils.VersionChecker?
        get() {
            val repoDatabase: RepoDatabase? = LitePal.find(appDatabaseId)
            val versionChecker = repoDatabase?.targetChecker
            return VersioningUtils.VersionChecker(versionChecker)
        }

    // 添加一个 更新检查器追踪子项
    private fun newEngine(databaseId: Long): JavaScriptEngine {
        val repoDatabase: RepoDatabase? = LitePal.find(databaseId)
        val apiUuid = repoDatabase?.api_uuid
        val url = repoDatabase?.url
        val apiName = HubDatabaseManager.getDatabase(apiUuid)?.name
        logObjectTag = arrayOf(apiName.toString(), databaseId.toString())
        // 查找软件源数据库
        val jsCode = HubDatabaseManager.getJsCode(apiUuid)
        Log.d(logObjectTag, TAG, "renewUpdateItem: uuid: $apiUuid")
        return JavaScriptEngine(logObjectTag, url, jsCode)
    }

    companion object {
        private val Log = ServerContainer.Log
        private const val TAG = "App"
    }
}