package net.xzos.upgradeall.core.manager

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater.Companion.APP_LATEST
import net.xzos.upgradeall.core.module.app.Updater.Companion.APP_NO_LOCAL
import net.xzos.upgradeall.core.module.app.Updater.Companion.APP_OUTDATED
import net.xzos.upgradeall.core.module.app.Updater.Companion.NETWORK_ERROR
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesCount
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.utils.coroutines.toCoroutinesMutableList


object AppManager {

    private val appMap = coroutinesMutableMapOf<Int, MutableList<App>>(true)

    private val appList = runBlocking { metaDatabase.appDao().loadAll() }.map { App(it) }
            .toCoroutinesMutableList(true)  // 存储所有 APP 实体

    fun getAppMap(): Map<Int, List<App>> = appMap

    fun getAppSortList(appType: String): List<App> {
        val list = appMap.get(APP_OUTDATED, mutableListOf()) +
                appMap.get(APP_NO_LOCAL, mutableListOf()) +
                appMap.get(APP_LATEST, mutableListOf()) +
                appMap.get(NETWORK_ERROR, mutableListOf())
        return list.filter { it.appId.containsKey(appType) }
    }

    fun getAppList(): List<App> {
        return appList
    }

    fun getAppList(appType: String): List<App> {
        return appList.filter { it.appId.containsKey(appType) }
    }

    suspend fun renewApp(renewStatusFun: ((renewingAppNum: Int) -> Unit)? = null) {
        val count = CoroutinesCount(appList.size)
        coroutineScope {
            for (app in appList)
                launch {
                    app.update()
                    appMap.get(app.getReleaseStatus(), mutableListOf())
                    count.down()
                    renewStatusFun?.run { this(count.count) }
                }
        }
    }

    suspend fun updateApp(appDatabase: AppEntity): AppEntity? {
        val appDao = metaDatabase.appDao()
        try {
            appDao.insert(appDatabase)
            // TODO: 错误类型判断，并给出 null 返回值
        } catch (ignore: SQLiteConstraintException) {
            appDao.update(appDatabase)
        }
        appList.add(App(appDatabase))
        return appDatabase
    }

    suspend fun removeApp(app: App) {
        metaDatabase.appDao().delete(app.appDatabase)
        appList.remove(app)
    }
}