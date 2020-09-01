package net.xzos.upgradeall.core.server_manager

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.oberver.Informer
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications


object AppManager : Informer {

    private val singleAppList = hashSetOf<App>() // 存储所有 APP 实体
    private val applicationsList = hashSetOf<Applications>()

    val apps: List<BaseApp>
        get() = (singleAppList + applicationsList).toList()

    init {
        // 从数据库初始化 APP 实例
        initApp()
    }

    private fun notifyChange() {
        notifyChanged()
    }

    private fun initApp() {
        AppDatabaseManager.appDatabases.map {
            setApp(it)
        }
        AppDatabaseManager.applicationsDatabases.map {
            setApplications(it)
        }
    }

    fun getBaseApp(databaseId: Long): BaseApp? {
        for (app in apps) {
            if (databaseId == app.appDatabase.id)
                return app
        }
        return null
    }

    fun getApplications(databaseId: Long? = null, hubUuid: String? = null): Applications? {
        for (applications in applicationsList) {
            if ((databaseId != null && applications.appDatabase.id == databaseId)
                    || (hubUuid != null && applications.appDatabase.hubUuid == hubUuid)
            ) return applications
        }
        return null
    }

    fun getSingleApp(databaseId: Long? = null, uuid: String? = null): App? {
        for (app in singleAppList) {
            if ((uuid != null && app.appDatabase.cloudConfig?.uuid == uuid)
                    || (databaseId != null && app.appDatabase.id == databaseId)
            ) return app
        }
        return null
    }

    fun setApplications(database: ApplicationsDatabase): Applications? {
        return getApplications(database.id)?.also {
            it.refreshData()
        } ?: Applications(database).also {
            if (applicationsList.add(it)) {
                notifyChange()
            }
        }
    }

    fun setApp(database: AppDatabase): App? {
        // 更新原有数据
        return getSingleApp(database.id)?.also {
            it.refreshData()
        } ?: App(database).also {
            if (singleAppList.add(it)) {
                notifyChange()
            }
        }
    }

    fun removeSingleApp(databaseId: Long? = null, app: App? = null) {
        val targetApp = getSingleApp(databaseId) ?: app
        if (targetApp != null) {
            singleAppList.remove(targetApp).let {
                if (it) notifyChange()
            }
        }
    }

    fun removeApplications(databaseId: Long? = null, applications: Applications? = null) {
        val targetApp = getApplications(databaseId) ?: applications
        if (targetApp != null) {
            applicationsList.remove(targetApp).let {
                if (it) notifyChange()
            }
        }
    }
}
