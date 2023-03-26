package net.xzos.upgradeall.core.manager

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.database.table.recheck
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.getInstalledAppList
import net.xzos.upgradeall.core.utils.oberver.Informer
import net.xzos.upgradeall.core.utils.oberver.Tag
import net.xzos.upgradeall.core.utils.registerAppReceiver


enum class UpdateStatus : Tag {
    APP_START_UPDATE_NOTIFY,
    APP_FINISH_UPDATE_NOTIFY,

    APP_UPDATE_STATUS_CHANGED_NOTIFY,
    APP_DATABASE_CHANGED_NOTIFY,

    APP_ADDED_NOTIFY,
    APP_DELETED_NOTIFY,
}

object AppManager : Informer<UpdateStatus, App>() {

    private val appMap = coroutinesMutableMapOf<AppStatus, CoroutinesMutableList<App>>(true)

    /**
     * 获取全部 App 实体列表
     */
    val appList: Set<App>
        get() = getAppList(AppStatus.APP_OUTDATED) +
                getAppList(AppStatus.APP_NO_LOCAL) +
                getAppList(AppStatus.APP_LATEST) +
                getAppList(AppStatus.APP_PENDING) +
                getAppList(AppStatus.NETWORK_ERROR)

    private val inactiveAppList: Set<App>
        get() = getAppList(AppStatus.APP_INACTIVE)

    fun initObject(context: Context) {
        runBlocking { renewAppList(context) }
        registerAppReceiver(context)
    }

    private suspend fun renewAppList(context: Context) {
        metaDatabase.appDao().loadAll().forEach { database ->
            addAppList(App(database), true)
        }
        getInstalledAppList(
            context, coreConfig.applications_ignore_system_app
        ).forEach { database ->
            addAppList(App(database), false)
        }
    }

    fun getAppList(hub: Hub): Set<App> {
        val list = mutableSetOf<App>()
        appList.forEach { app ->
            app.hubEnableList.forEach {
                if (it == hub) list.add(app)
            }
        }
        return list
    }

    fun getAppList(key: AppStatus): Set<App> {
        return appMap[key]?.toSet() ?: emptySet()
    }

    private fun removeAppList(app: App) {
        if (removeAppMapExclude(app, AppStatus.APP_INACTIVE)) {
            notifyChanged(UpdateStatus.APP_DELETED_NOTIFY, app)
        }
    }

    private fun addAppMap(
        key: AppStatus, app: App
    ): Boolean {
        return appMap.getOrPut(key) { coroutinesMutableListOf(true) }.add(app)
    }

    private fun removeAppMapExclude(
        app: App, vararg excludeKey: AppStatus
    ): Boolean {
        var deleted = false
        appMap.forEach {
            if (!excludeKey.contains(it.key) && it.value.remove(app)) {
                deleted = true
                if (it.value.isEmpty()) appMap.remove(it.key)
            }
        }
        return deleted
    }

    private fun removeAppMap(key: AppStatus, app: App): Boolean {
        return appMap[key]?.run {
            remove(app).also {
                if (it && isEmpty()) appMap.remove(key)
            }
        } ?: false
    }

    private fun addAppList(app: App, noCheck: Boolean = false) {
        if ((noCheck || app.isActive) && addAppMap(AppStatus.APP_PENDING, app)) {
            notifyChanged(UpdateStatus.APP_ADDED_NOTIFY, app)
        } else {
            addAppMap(AppStatus.APP_INACTIVE, app)
        }
    }

    /**
     * 按照更新状态{@link Updater}排序的列表
     * 在完成数据刷新{@link #renewApp}后，可以通过 App 类型{@link Constant}获取该类型的列表
     */
    fun getAppByUuid(uuid: String): App? {
        appList.forEach {
            if (uuid == it.cloudConfig?.uuid)
                return it
        }
        return null
    }

    /**
     * 获取全部 App 实体列表，按照 App 类型过滤
     */
    fun getAppList(appType: String): List<App> {
        return appList.filter { it.appId.containsKey(appType) }
    }

    /**
     * 刷新 App 的版本数据
     * @param renewStatusFun 每刷新一个 App 数据，回调一次，以返回正在刷新中的 App 数量
     */
    suspend fun renewApp(
        renewStatusFun: ((renewingAppNum: Int, totalAppNum: Int) -> Unit)? = null,
        renewInactiveStatusFun: ((renewingAppNum: Int, totalAppNum: Int) -> Unit)? = null,
    ): Int {
        renewAppList(appList, renewStatusFun)
        renewAppList(inactiveAppList, renewInactiveStatusFun)
        return appList.size
    }

    private suspend fun renewAppList(
        appList: Collection<App>,
        statusFun: ((renewingAppNum: Int, totalAppNum: Int) -> Unit)? = null,
    ): Collection<App> {
        val count = CoroutinesCount(appList.size)
        val totalAppNum = appList.size
        Log.w("update record", "renew size start: $totalAppNum")
        coroutineScope {
            for (app in appList) {
                launch(Dispatchers.IO) {
                    renewApp(app)
                    count.down()
                    Log.w("update record", "count: ${count.count}, app: ${app.appId}")
                    statusFun?.run { this(count.count, totalAppNum) }
                }
            }
        }
        Log.w("update record", "renew size finish: $totalAppNum")
        return appList
    }

    /**
     * 刷新指定 App 项的版本数据
     * @param app 需要重新刷新的 App 项
     */
    fun renewApp(app: App) {
        notifyChanged(UpdateStatus.APP_START_UPDATE_NOTIFY, app)
        app.update()
        if (checkActiveApp(app))
            setAppMap(app)
        notifyChanged(UpdateStatus.APP_FINISH_UPDATE_NOTIFY, app)
    }

    private fun checkActiveApp(app: App): Boolean {
        if (!app.db.isInit()) {
            return if (app.releaseStatus == AppStatus.NETWORK_ERROR) {
                addAppMap(AppStatus.APP_INACTIVE, app)
                removeAppList(app)
                false
            } else {
                removeAppMap(AppStatus.APP_INACTIVE, app)
                addAppList(app, true)
                true
            }
        }
        return true
    }

    private fun setAppMap(app: App) {
        val releaseStatus = app.releaseStatus

        // check changed
        var changed = false
        // reset app map

        // retry add
        if (addAppMap(releaseStatus, app))
            changed = true

        // retry delete
        if (removeAppMap(releaseStatus, app))
            changed = true

        // check changed
        if (changed)
            notifyChanged(UpdateStatus.APP_UPDATE_STATUS_CHANGED_NOTIFY, app)
    }

    private fun getAppByDatabase(appEntity: AppEntity): App? {
        appList.forEach {
            if (it.db == appEntity)
                return it
        }
        return null
    }

    fun getAppById(appId: Map<String, String?>): App? {
        appList.forEach {
            if (appId == it.appId)
                return it
        }
        return null
    }

    /**
     * 用数据库数据修改数据库并更新 App 数据
     */
    suspend fun saveApp(appDatabase: AppEntity): App? {
        appDatabase.recheck()
        return updateApp(addAppEntity(appDatabase) ?: return null)
    }

    private suspend fun addAppEntity(appEntity: AppEntity): AppEntity? {
        val appDao = metaDatabase.appDao()
        return try {
            if (appEntity.isInit())
                appDao.update(appEntity)
            else
                appDao.insert(appEntity)
            appEntity
            // TODO: 错误类型判断，并给出 null 返回值
        } catch (ignore: SQLiteConstraintException) {
            null
        }
    }

    private fun updateApp(appDatabase: AppEntity): App {
        val oldApp = getAppByDatabase(appDatabase) ?: getAppById(appDatabase.appId)
        val changedTag = if (oldApp != null)
            UpdateStatus.APP_DATABASE_CHANGED_NOTIFY
        else UpdateStatus.APP_ADDED_NOTIFY
        val app = oldApp ?: App(appDatabase).apply {
            addAppList(this, false)
        }
        renewApp(app)
        notifyChanged(changedTag, app)
        return app
    }

    /**
     * 删除这个 App 项，包括存储其数据的数据库行
     */
    suspend fun removeApp(app: App) {
        metaDatabase.appDao().delete(app.db)
        removeAppList(app)
        removeAppMap(AppStatus.APP_INACTIVE, app)
    }
}