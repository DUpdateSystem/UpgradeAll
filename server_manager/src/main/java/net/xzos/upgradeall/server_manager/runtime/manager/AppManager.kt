package net.xzos.upgradeall.server_manager.runtime.manager

import net.xzos.upgradeall.data.database.AppDatabase
import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.data_manager.AppDatabaseManager
import net.xzos.upgradeall.server_manager.runtime.manager.module.app.App
import net.xzos.upgradeall.system_api.annotations.DatabaseApi


object AppManager {

    init {
        net.xzos.upgradeall.system_api.api.DatabaseApi.register(this)
    }

    private val singleAppSet = hashSetOf<App>() // 存储所有 APP 实体

    private val allAppSet = hashSetOf<App>()

    val apps: HashSet<App>
        get() = singleAppSet

    init {
        // 从数据库初始化 APP 实例
        initApp()
        // 同步数据
        allAppSet.addAll(singleAppSet)
    }

    private fun initApp() {
        val appDatabase = AppDatabaseManager.appDatabases
        for (appItem in appDatabase) {
            setApp(appItem)
        }
    }

    @DatabaseApi.databaseChanged
    fun refalshData(database: Any) {
        if (database is AppDatabase) {
            val databaseId = database.id
            if (AppDatabaseManager.exists(databaseId)) {
                addSingleApp(databaseId)
            } else {
                getSingleApp(databaseId = databaseId)?.run {
                    removeSingleApp(this)
                }
            }
        } else if (database is HubDatabase) {
            val hubUuid = database.uuid
            renewAppInHub(hubUuid)
        }
    }

    fun getSingleApp(databaseId: Long? = null, uuid: String? = null): App? {
        for (app in singleAppSet) {
            if ((uuid != null && app.appInfo.extraData?.cloudAppConfigGson?.uuid == uuid)
                    || (databaseId != null && app.appInfo.id == databaseId))
                return app
        }
        return null
    }

    private fun setApp(appDatabase: AppDatabase) {
        singleAppSet.add(App(appDatabase))
    }

    fun addSingleApp(databaseId: Long? = null, app: App? = null): Boolean {
        // 具有查询重复功能（设计中一般不会有重复）
        if (databaseId != null) {
            val databaseIdList = singleAppSet.map {
                it.appInfo.id
            }
            // 删除原有数据
            if (databaseIdList.contains(databaseId)) {
                getSingleApp(databaseId)?.run {
                    removeSingleApp(this)
                }
            }
            // 尝试添加新数据
            AppDatabaseManager.getDatabase(databaseId = databaseId)?.run {
                // 创建并添加应用
                setApp(this)
                return true
            }
        } else if (app != null && app.appInfo.id != 0L) {
            // 尝试添加列表
            return singleAppSet.add(app)
        }
        return false
    }

    fun addApps(appList: HashSet<App>) {
        allAppSet.addAll(appList)
    }

    private fun removeSingleApp(app: App) {
        // TODO: initApp自维护，数据来源：独立 UI 数据
        singleAppSet.remove(app)
    }

    private fun renewAppInHub(hubUuid: String) {
        for (app in singleAppSet) {
            if (app.appInfo.api_uuid == hubUuid) {
                app.renewEngine()
            }
        }
    }
}
