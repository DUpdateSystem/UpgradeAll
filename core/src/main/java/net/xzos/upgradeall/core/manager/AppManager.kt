package net.xzos.upgradeall.core.manager

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.isInit
import net.xzos.upgradeall.core.database.table.recheck
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater.Companion.APP_LATEST
import net.xzos.upgradeall.core.module.app.Updater.Companion.APP_NO_LOCAL
import net.xzos.upgradeall.core.module.app.Updater.Companion.APP_OUTDATED
import net.xzos.upgradeall.core.module.app.Updater.Companion.NETWORK_ERROR
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

object AppManager : Informer {

    private val appMap = coroutinesMutableMapOf<Int, CoroutinesMutableList<App>>(true)


    // 存储所有 APP 实体
    private val appList = coroutinesMutableListOf<App>(true)
    private val inactiveAppList = coroutinesMutableListOf<App>(true)


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

    private fun getAppList(key: Int): CoroutinesMutableList<App> {
        return appMap.getOrDefault(key) { coroutinesMutableListOf(true) }
    }

    private fun removeAppList(app: App) {
        if (appList.remove(app))
            notifyChanged(UpdateStatus.APP_DELETED_NOTIFY, app)
    }

    private fun addAppList(app: App, noCheck: Boolean = false) {
        if (noCheck && appList.add(app)) {
            notifyChanged(UpdateStatus.APP_ADDED_NOTIFY, app)
        } else if (app.isActive && appList.add(app)) {
            notifyChanged(UpdateStatus.APP_ADDED_NOTIFY, app)
        } else {
            inactiveAppList.add(app)
        }
    }

    /**
     * 以更新状态{@link Updater}为键值的字典
     * 在完成数据刷新{@link #renewApp}后，可以通过 App 类型{@link Constant}获取可以通过该字典来查看各个更新状态的 App 列表
     */
    fun getAppMap(appType: String? = null): Map<Int, List<App>> {
        return if (appType != null) {
            appMap.map {
                it.key to it.value.filter { it.appId.containsKey(appType) }
            }.toMap()
        } else appMap
    }

    /**
     * 按照更新状态{@link Updater}排序的列表
     * 在完成数据刷新{@link #renewApp}后，可以通过 App 类型{@link Constant}获取该类型的列表
     */
    fun getAppSortList(appType: String): List<App> {
        val list = getAppList(APP_OUTDATED) +
                getAppList(APP_NO_LOCAL) +
                getAppList(APP_LATEST) +
                getAppList(NETWORK_ERROR)
        return list.filter { it.appId.containsKey(appType) }
    }

    /**
     * 获取全部 App 实体列表
     */
    fun getAppList(): List<App> {
        return appList
    }

    fun getUserAppList(): List<App> {
        return appList.filter { !it.isVirtual }
    }

    fun getAutoAppList(): List<App> {
        return appList.filter { it.isVirtual }
    }

    fun getAppListWithoutKey(excludeAppType: String): List<App> {
        return appList.filter { !it.appId.containsKey(excludeAppType) }
    }

    fun getAppByUuid(uuid: String): App? {
        appList.forEach {
            if (uuid == it.appDatabase.cloudConfig?.uuid)
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
        appList: List<App>,
        statusFun: ((renewingAppNum: Int, totalAppNum: Int) -> Unit)? = null,
    ): List<App> {
        val count = CoroutinesCount(appList.size)
        val totalAppNum = appList.size
        Log.w("update record", "renew size start: $totalAppNum")
        coroutineScope {
            for (app in appList) {
                launch {
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
    suspend fun renewApp(app: App) {
        notifyChanged(UpdateStatus.APP_START_UPDATE_NOTIFY, app)
        app.update()
        if (checkActiveApp(app))
            setAppMap(app)
        notifyChanged(UpdateStatus.APP_FINISH_UPDATE_NOTIFY, app)
    }

    private fun checkActiveApp(app: App): Boolean {
        if (!app.appDatabase.isInit()) {
            return if (app.getReleaseStatus() == NETWORK_ERROR) {
                appMap.forEach { it.value.remove(app) }
                inactiveAppList.add(app)
                removeAppList(app)
                false
            } else {
                inactiveAppList.remove(app)
                addAppList(app, true)
                true
            }
        }
        return true
    }

    private fun setAppMap(app: App) {
        val releaseStatus = app.getReleaseStatus()
        var changed = false
        // reset app map

        // retry add
        getAppList(releaseStatus).run {
            if (!contains(app)) {
                add(app)
                changed = true
            }
        }

        // retry delete
        appMap.forEach {
            if (it.key != releaseStatus) {
                it.value.remove(app)
                changed = true
            }
        }

        // check changed
        if (changed) {
            notifyChanged(UpdateStatus.APP_UPDATE_STATUS_CHANGED_NOTIFY, app)
        }
    }

    private fun getAppByDatabase(appEntity: AppEntity): App? {
        appList.forEach {
            if (it.appDatabase.id == appEntity.id)
                return it
        }
        return null
    }

    fun getAppById(appId: Map<String, String?>): App? {
        appList.forEach {
            if (appId == it.appDatabase.appId)
                return it
        }
        return null
    }

    /**
     * 用数据库数据修改数据库并更新 App 数据
     */
    suspend fun updateApp(appDatabase: AppEntity): App? {
        appDatabase.recheck()
        return addAppMap(addAppEntity(appDatabase) ?: return null)
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

    private suspend fun addAppMap(appDatabase: AppEntity): App {
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
        metaDatabase.appDao().delete(app.appDatabase)
        appMap.forEach {
            it.value.remove(app)
        }
        removeAppList(app)
    }

    override val informerId: Int = Informer.getInformerId()
}