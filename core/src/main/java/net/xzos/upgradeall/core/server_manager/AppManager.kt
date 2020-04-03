package net.xzos.upgradeall.core.server_manager

import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications


object AppManager {

    private val singleAppList = mutableListOf<App>() // 存储所有 APP 实体
    private val applicationsList = mutableListOf<Applications>()

    val apps: List<BaseApp>
        get() = singleAppList + applicationsList

    init {
        // 从数据库初始化 APP 实例
        initApp()
    }

    private fun initApp() {
        val appDatabase = AppDatabaseManager.appDatabases
        for (appItem in appDatabase) {
            when (appItem.type) {
                AppDatabase.APP_TYPE_TAG -> setApp(appItem)
                AppDatabase.APPLICATIONS_TYPE_TAG -> setApplications(appItem)
            }
        }
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
            if ((uuid != null && app.appDatabase.extraData?.cloudAppConfigGson?.uuid == uuid)
                || (databaseId != null && app.appDatabase.id == databaseId)
            ) return app
        }
        return null
    }

    private fun setApplications(appDatabase: AppDatabase): Applications? {
        if (appDatabase.type == AppDatabase.APPLICATIONS_TYPE_TAG) {
            removeApplications(appDatabase.id)
            val applications = Applications(appDatabase)
            if (applicationsList.add(applications))
                return applications
        }
        return null
    }

    private fun setApp(appDatabase: AppDatabase): App? {
        if (appDatabase.type == AppDatabase.APP_TYPE_TAG) {
            // 删除原有数据
            removeSingleApp(appDatabase.id)
            // 尝试添加新数据
            val app = App(appDatabase)
            if (singleAppList.add(app))
                return app
        }
        return null
    }

    private fun removeSingleApp(databaseId: Long? = null, app: App? = null) {
        val targetApp = getSingleApp(databaseId) ?: app
        if (targetApp != null)
            singleAppList.remove(targetApp)
    }

    private fun removeApplications(databaseId: Long? = null, applications: Applications? = null) {
        val targetApp = getApplications(databaseId) ?: applications
        if (targetApp != null)
            applicationsList.remove(targetApp)
    }

    /**
     * 检查数据库数据以刷新 APP 实体
     */
    internal fun refreshData(database: AppDatabase) {
        val databaseId = database.id
        val appDatabase = AppDatabaseManager.getDatabase(databaseId)
        if (appDatabase != null) {
            when (appDatabase.type) {
                AppDatabase.APP_TYPE_TAG -> setApp(appDatabase)
                AppDatabase.APPLICATIONS_TYPE_TAG -> setApplications(appDatabase)
            }
        } else {
            removeSingleApp(databaseId)
            removeApplications(databaseId)
        }
    }
}
