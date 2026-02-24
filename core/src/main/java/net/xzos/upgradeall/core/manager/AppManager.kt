package net.xzos.upgradeall.core.manager

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.utils.getInstalledAppList
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
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
    private const val TAG = "AppManager"
    private val logObjectTag = ObjectTag(core, TAG)

    /**
     * 获取全部 App 实体列表
     */
    private val allAppList = coroutinesMutableListOf<App>(true)

    // -------------------------------------------------------------------------
    // Rust renew pipeline callbacks
    // -------------------------------------------------------------------------

    @Volatile private var currentRenewProgressFun: ((done: Int, total: Int) -> Unit)? = null

    fun setRenewProgressFun(f: ((Int, Int) -> Unit)?) {
        currentRenewProgressFun = f
    }

    /** Called by core/Init.kt when Rust fires a renew_progress event. */
    fun notifyRenewProgress(done: Int, total: Int) {
        currentRenewProgressFun?.invoke(done, total)
    }

    /** Called by core/Init.kt when Rust fires an app_status_changed event. */
    fun notifyAppStatusChanged(appId: Map<String, String?>) {
        val app = allAppList.firstOrNull { it.appId == appId } ?: return
        notifyChanged(UpdateStatus.APP_FINISH_UPDATE_NOTIFY, app)
        notifyChanged(UpdateStatus.APP_UPDATE_STATUS_CHANGED_NOTIFY, app)
    }

    private val inactiveAppList: Set<App>
        get() = getAppList(AppStatus.APP_INACTIVE)

    fun getAppList(predicate: ((App) -> Boolean) = { true }): Set<App> =
        getUnsortedAppList(predicate)
            .toSortedSet { o1, o2 ->
                o1.name.compareTo(o2.name)
            }

    private fun getUnsortedAppList(predicate: ((App) -> Boolean) = { true }): Set<App> =
        allAppList
            .filter { (it.releaseStatus != AppStatus.APP_INACTIVE) && predicate(it) }
            .toSet()

    fun initObject(context: Context) {
        runBlocking { renewAppList(context) }
        registerAppReceiver(context)
    }

    private suspend fun renewAppList(context: Context) {
        metaDatabase.appDao().loadAll().forEach { database ->
            allAppList.add(App(database))
        }
        getInstalledAppList(
            context,
            coreConfig.applications_ignore_system_app,
        ).forEach { database ->
            allAppList.add(App(database))
        }
    }

    fun getAppList(hub: Hub): Set<App> {
        val list = mutableSetOf<App>()
        allAppList.forEach { app ->
            app.hubEnableList.forEach {
                if (it == hub) list.add(app)
            }
        }
        return list
    }

    fun getAppList(key: AppStatus): Set<App> = allAppList.filter { it.releaseStatus == key }.toSet()

    /**
     * 按照更新状态{@link Updater}排序的列表
     * 在完成数据刷新{@link #renewApp}后，可以通过 App 类型{@link Constant}获取该类型的列表
     */
    fun getAppByUuid(uuid: String): App? {
        allAppList.forEach {
            if (uuid == it.cloudConfig?.uuid) {
                return it
            }
        }
        return null
    }

    /**
     * 获取全部 App 实体列表，按照 App 类型过滤
     */
    fun getAppList(appType: String): Set<App> = allAppList.filter { it.appId.containsKey(appType) }.toSet()

    /**
     * 刷新指定 App 项的版本数据
     * @param app 需要重新刷新的 App 项
     */
    fun renewApp(app: App) {
        notifyChanged(UpdateStatus.APP_START_UPDATE_NOTIFY, app)
        DataGetter.getLatestVersion(app)
        notifyChanged(UpdateStatus.APP_FINISH_UPDATE_NOTIFY, app)
    }

    fun renewApp(
        app: App,
        hub: Hub,
    ) {
        DataGetter.getLatestVersion(app, hub)
    }

    private fun getAppByDatabase(appEntity: AppEntity): App? {
        allAppList.forEach {
            if (it.db == appEntity) {
                return it
            }
        }
        return null
    }

    fun getAppById(appId: Map<String, String?>): App? {
        allAppList.forEach {
            if (appId == it.appId) {
                return it
            }
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
            if (appEntity.isInit()) {
                appDao.update(appEntity)
            } else {
                appDao.insert(appEntity)
            }
            appEntity
            // TODO: 错误类型判断，并给出 null 返回值
        } catch (ignore: SQLiteConstraintException) {
            null
        }
    }

    private fun updateApp(appDatabase: AppEntity): App {
        val oldApp = getAppByDatabase(appDatabase) ?: getAppById(appDatabase.appId)
        val changedTag =
            if (oldApp != null) {
                UpdateStatus.APP_DATABASE_CHANGED_NOTIFY
            } else {
                UpdateStatus.APP_ADDED_NOTIFY
            }
        val app =
            oldApp ?: App(appDatabase).apply {
                allAppList.add(this)
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
        allAppList.remove(app)
    }
}
