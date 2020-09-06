package net.xzos.upgradeall.core.data_manager

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.data.database.HubDatabase
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.system_api.api.DatabaseApi


object HubDatabaseManager {

    private const val TAG = "HubDatabaseManager"
    private val objectTag = ObjectTag("Core", TAG)

    // 读取 hub 数据库
    val hubDatabases: HashSet<HubDatabase> = runBlocking {
        DatabaseApi?.getHubDatabaseList()?.toHashSet() ?: hashSetOf()
    }

    fun getDatabase(uuid: String?): HubDatabase? {
        var hubDatabase: HubDatabase? = null
        for (database in hubDatabases) {
            if (database.uuid == uuid) {
                hubDatabase = database
            }
        }
        return hubDatabase
    }

    fun exists(uuid: String?) = getDatabase(
            uuid
    ) != null

    suspend fun insertDatabase(database: HubDatabase): Boolean {
        return (DatabaseApi?.insertHubDatabase(database) != null).also {
            if (it) hubDatabases.add(database)
        }
    }

    suspend fun updateDatabase(database: HubDatabase): Boolean {
        return (DatabaseApi?.updateHubDatabase(database) != true).also {
            if (it) hubDatabases.add(database)
        }
    }

    suspend fun deleteDatabase(database: HubDatabase): Boolean {
        return DatabaseApi?.deleteHubDatabase(database) ?: false
    }


    suspend fun addDatabase(hubConfigGsonGson: HubConfigGson): Boolean {
        val name: String? = hubConfigGsonGson.info.hubName
        val uuid: String? = hubConfigGsonGson.uuid

        // 如果设置了名字与 UUID，则存入数据库
        if (name != null && uuid != null) {
            // 修改数据库
            getDatabase(uuid)?.also {
                it.uuid = uuid
                it.hubConfig = hubConfigGsonGson
                return DatabaseApi?.updateHubDatabase(it) ?: false
            } ?: HubDatabase(uuid, hubConfigGsonGson).also {
                return DatabaseApi?.insertHubDatabase(it) != 0L
            }
            // 存储 js 代码
            // 将数据存入 HubDatabase 数据库
        }
        return false
    }
}
