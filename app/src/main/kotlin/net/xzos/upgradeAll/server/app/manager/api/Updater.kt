package net.xzos.upgradeAll.server.app.manager.api

import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.EngineApi
import net.xzos.upgradeAll.server.app.engine.js.JavaScriptEngine
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.viewmodels.componnent.EditIntPreference
import org.json.JSONObject
import org.litepal.LitePal
import org.litepal.extension.find
import java.util.*


class Updater internal constructor(private val appDatabaseId: Int) {
    private var engine: EngineApi = newEngine((appDatabaseId))
    val renewing = MutableLiveData(false)

    val isSuccessRenew: Boolean
        get() = engine.releaseNum != 0

    // 获取最新版本号
    val latestVersion: String?
        get() = engine.getVersioning(0)

    // 获取最新下载链接
    val latestDownloadUrl: JSONObject
        get() = engine.getReleaseDownload(0)

    fun renew(isAuto: Boolean) {
        if (isAuto) {
            autoRenew()
        } else {
            forcedRenew()
        }
    }

    /**
     * 检查刷新时间，
     * 如果时间未到，
     * 则停止刷新
     */
    private fun autoRenew() {
        // 检查更新时间更新数据
        var startRefresh = true
        val defaultDataExpirationTime = MyApplication.context.resources.getInteger(R.integer.default_data_expiration_time)  // 默认自动刷新时间 10min
        val autoRefreshMinute = EditIntPreference.getInt("sync_time", defaultDataExpirationTime)
        val updateTime: Calendar? = engine.renewTime
        if (updateTime != null) {
            updateTime.add(Calendar.MINUTE, autoRefreshMinute)
            if (Calendar.getInstance().before(updateTime)) {
                Log.v(LogObjectTag, TAG, String.format("autoRefreshAll: %s NoUp", appDatabaseId))
                startRefresh = false
            }
        }
        if (startRefresh)
            forcedRenew()
    }

    private fun forcedRenew() {
        Thread(Runnable { this.refreshThread() }).start()
    }

    /**
     * 刷新数据
     */
    private fun refreshThread() {
        renewing.postValue(true)
        engine.refreshData()
        // 检查刷新
        if (isSuccessRenew) {
            engine.setRenewTime()
            Log.v(LogObjectTag, TAG, "refreshThread: 刷新成功")
        }
        renewing.postValue(false)
    }

    private fun newEngine(databaseId: Int): EngineApi {
        // 添加一个 更新检查器追踪子项
        val repoDatabase: RepoDatabase = LitePal.find(databaseId.toLong())
                ?: return EngineApi.emptyEngine
        val apiUuid = repoDatabase.api_uuid
        Log.d(LogObjectTag, TAG, "renewUpdateItem: uuid: $apiUuid")
        val url = repoDatabase.url
        val apiName = repoDatabase.api
        val logObjectTag = arrayOf(apiName, databaseId.toString())
        // 查找软件源数据库
        val jsCode = HubManager.getJsCode(apiUuid)
        return JavaScriptEngine(logObjectTag, url, jsCode)
    }

    companion object {

        private val Log = ServerContainer.AppServer.log
        private const val TAG = "Updater"
        private val LogObjectTag = arrayOf("Core", TAG)
    }
}
