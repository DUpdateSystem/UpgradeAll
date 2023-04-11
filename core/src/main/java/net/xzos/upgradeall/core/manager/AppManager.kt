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
import net.xzos.upgradeall.core.module.app.data.DataGetter
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
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

    private val appList = coroutinesMutableListOf<App>(true)

    /**
     * 获取全部 App 实体列表
     */
    val appMap: Map<AppStatus, Collection<App>>
        get() {
            val map = mutableMapOf<AppStatus, MutableList<App>>()
            appList.forEach {
                map.getOrPut(it.releaseStatus) { mutableListOf(it) }
            }
            return map
        }

    private val inactiveAppList: Set<App>
        get() = getAppList(AppStatus.APP_INACTIVE)

    fun initObject(context: Context) {
        runBlocking { renewAppList(context) }
        registerAppReceiver(context)
    }

    private suspend fun renewAppList(context: Context) {
        metaDatabase.appDao().loadAll().forEach { database ->
            appList.add(App(database))
        }
        getInstalledAppList(
            context, coreConfig.applications_ignore_system_app
        ).forEach { database ->
            appList.add(App(database))
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

    fun getSortAppList(): Set<App> =
        getAppList(AppStatus.APP_OUTDATED) +
                getAppList(AppStatus.APP_NO_LOCAL) +
                getAppList(AppStatus.APP_LATEST) +
                getAppList(AppStatus.APP_PENDING) +
                getAppList(AppStatus.NETWORK_ERROR)

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
        val simpleMap = coroutinesMutableMapOf<Hub, MutableList<App>>(true)
        val completeMap = coroutinesMutableMapOf<Hub, MutableList<App>>(true)
        val appMap = coroutinesMutableMapOf<App, MutableList<Hub>>(true)
        val appStatusMap = coroutinesMutableMapOf<App, AppStatus>(true)
        coroutineScope {
            appList.forEach {
                launch {
                    notifyChanged(UpdateStatus.APP_START_UPDATE_NOTIFY, it)
                    appStatusMap[it] = it.releaseStatus
                    it.hubEnableList.forEach { hub ->
                        val map = if (it.needCompleteVersion) completeMap else simpleMap
                        map.getOrPut(hub) { mutableListOf() }.add(it)
                    }
                    appMap[it] = it.hubEnableList.toMutableList()
                }
            }
        }
        coroutineScope {
            simpleMap.forEach { i ->
                val (hub, list) = i
                launch {
                    val flashApp = DataGetter.getLatestUpdate(hub, list)
                    if (flashApp.isEmpty())
                        completeMap.getOrPut(hub) { mutableListOf() }.addAll(list)
                    else {
                        flashApp.forEach {
                            checkUpdated(
                                it, hub, appMap, appStatusMap, count, totalAppNum, statusFun
                            )
                            list.remove(it)
                        }
                        list.forEach { completeMap.getOrPut(hub) { mutableListOf() }.add(it) }
                    }
                }
            }
        }

        coroutineScope {
            completeMap.forEach { i ->
                val (hub, list) = i
                list.forEach {
                    launch(Dispatchers.IO) {
                        renewApp(it, hub)
                        checkUpdated(it, hub, appMap, appStatusMap, count, totalAppNum, statusFun)
                    }
                }
            }
        }
        Log.w("update record", "renew size finish: $totalAppNum")
        return appList
    }

    private fun checkUpdated(
        app: App, hub: Hub, map: MutableMap<App, MutableList<Hub>>,
        appStatusMap: MutableMap<App, AppStatus>, count: CoroutinesCount, totalAppNum: Int,
        statusFun: ((renewingAppNum: Int, totalAppNum: Int) -> Unit)?,
    ) {
        val list = map[app] ?: return
        if (list.contains(hub)) {
            if (list.size == 1) {
                map.remove(app)
                notifyChanged(UpdateStatus.APP_FINISH_UPDATE_NOTIFY, app)
                if (appStatusMap[app] != app.releaseStatus)
                    notifyChanged(UpdateStatus.APP_UPDATE_STATUS_CHANGED_NOTIFY, app)
                count.down()
                Log.w("update record", "count: ${count.count}, app: ${app.appId}")
                statusFun?.run { this(count.count, totalAppNum) }
            } else {
                list.remove(hub)
            }
        }
    }

    /**
     * 刷新指定 App 项的版本数据
     * @param app 需要重新刷新的 App 项
     */
    fun renewApp(app: App) {
        notifyChanged(UpdateStatus.APP_START_UPDATE_NOTIFY, app)
        DataGetter.getLatestVersion(app)
        notifyChanged(UpdateStatus.APP_FINISH_UPDATE_NOTIFY, app)
    }

    fun renewApp(app: App, hub: Hub) {
        DataGetter.getLatestVersion(app, hub)
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
            appList.add(this)
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
        appList.remove(app)
    }
}