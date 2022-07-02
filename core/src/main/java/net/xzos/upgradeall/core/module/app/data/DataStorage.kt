package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.getEnableSortHubList
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.websdk.getServerApi

internal data class DataStorage(
    val appDatabase: AppEntity,
) {
    // 刷新状态锁
    val renewMutex = Mutex()

    // 可用软件源列表
    val hubList: List<Hub>
        get() = appDatabase.getEnableSortHubList().filter { it.isValidApp(appDatabase.appId) }

    // Version 信息
    val versionMap: VersionMap =
        runBlocking { VersionMap.new(appDatabase.invalidVersionNumberFieldRegexString) }

    val serverApi = getServerApi()
}