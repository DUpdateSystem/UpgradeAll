package net.xzos.upgradeall.data_manager

import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.data.json.gson.HubConfig
import net.xzos.upgradeall.data.json.gson.HubDatabaseExtraData
import net.xzos.upgradeall.data.json.nongson.ObjectTag
import net.xzos.upgradeall.system_api.api.DatabaseApi


object HubDatabaseManager {

    private const val TAG = "HubDatabaseManager"
    private val objectTag = ObjectTag("Core", TAG)

    // 读取 hub 数据库
    val hubDatabases: List<HubDatabase>
        get() = DatabaseApi.hubDatabases

    fun getDatabase(uuid: String?): HubDatabase? {
        var hubDatabase: HubDatabase? = null
        for (database in hubDatabases) {
            if (database.uuid == uuid) {
                hubDatabase = database
            }
        }
        return hubDatabase
    }

    fun exists(uuid: String?) = getDatabase(uuid) != null

    fun saveDatabase(hubDatabase: HubDatabase): Boolean {
        return DatabaseApi.saveHubDatabase(hubDatabase)
    }

    fun deleteDatabase(hubDatabase: HubDatabase): Boolean {
        return DatabaseApi.deleteHubDatabase(hubDatabase)
    }


    fun addDatabase(hubConfigGson: HubConfig, jsCode: String): Boolean {
        val name: String? = hubConfigGson.info?.hubName
        val uuid: String? = hubConfigGson.uuid

        // 如果设置了名字与 UUID，则存入数据库
        if (name != null && uuid != null) {
            val hubDatabaseExtraData = HubDatabaseExtraData(jsCode)
            // 修改数据库
            (getDatabase(uuid)
                    ?: HubDatabase.newInstance()).apply {
                this.name = name
                this.uuid = uuid
                this.cloudHubConfig = hubConfigGson
                // 存储 js 代码
                this.extraData = hubDatabaseExtraData
            }.run {
                saveDatabase(this) // 将数据存入 HubDatabase 数据库
            }
            return true
        }
        return false
    }

    fun getJsCode(uuid: String?): String? {
        return getDatabase(uuid)?.extraData?.javascript
    }
}
