package net.xzos.upgradeall.server_manager.runtime.manager.module.app

import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.data_manager.database.AppDatabase
import net.xzos.upgradeall.data_manager.database.manager.HubDatabaseManager
import net.xzos.upgradeall.jscore.JSEngine
import net.xzos.upgradeall.jscore.js.engine.JavaScriptEngine
import net.xzos.upgradeall.log.Log
import net.xzos.upgradeall.system_api.api.IoApi


class App(val appInfo: AppDatabase) {
    private lateinit var objectTag: ObjectTag
    var engine = newEngine()

    val markProcessedVersionNumber: String?
        get() = appInfo.extraData?.markProcessedVersionNumber

    fun renewEngine() {
        newEngine()
    }

    // 获取已安装版本号
    val installedVersionNumber: String?
        get() = IoApi.getAppVersionNumber(appInfo.targetChecker)

    // 添加一个 更新检查器追踪子项
    private fun newEngine(): JavaScriptEngine {
        val apiUuid = appInfo.api_uuid
        val url = appInfo.url
        val apiName = HubDatabaseManager.getDatabase(apiUuid)?.name
        objectTag = ObjectTag(apiName.toString(), appInfo.name)
        // 查找软件源数据库
        val jsCode = HubDatabaseManager.getJsCode(apiUuid)
        Log.d(objectTag, TAG, "renewUpdateItem: uuid: $apiUuid")
        return JSEngine(objectTag, url, jsCode, false).javaScriptEngine
    }

    companion object {
        private const val TAG: String = "App"
    }
}
