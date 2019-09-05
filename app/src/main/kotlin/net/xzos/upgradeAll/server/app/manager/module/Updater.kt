package net.xzos.upgradeAll.server.app.manager.module

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.CoreApi
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.hub.HubManager
import org.json.JSONObject
import org.litepal.LitePal
import org.litepal.extension.find


class Updater internal constructor(appDatabaseId: Int) {
    private val engine = newEngine((appDatabaseId))

    val isSuccessRenew: Boolean
        get() {
            return runBlocking {
                engine.getVersioning(0) != null
            }
        }

    // 获取最新版本号
    val latestVersion: String?
        get() {
            return runBlocking {
                engine.getVersioning(0)
            }
        }

    // 获取最新下载链接
    val latestDownloadUrl: JSONObject
        get() {
            return runBlocking {
                engine.getReleaseDownload(0)
            }
        }

    private fun newEngine(databaseId: Int): CoreApi {
        // 添加一个 更新检查器追踪子项
        val repoDatabase: RepoDatabase? = LitePal.find(databaseId.toLong())
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
