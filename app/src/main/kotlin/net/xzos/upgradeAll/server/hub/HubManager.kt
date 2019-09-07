package net.xzos.upgradeAll.server.hub

import com.google.gson.Gson

import net.xzos.upgradeAll.database.HubDatabase
import net.xzos.upgradeAll.json.gson.HubConfig
import net.xzos.upgradeAll.json.gson.HubDatabaseExtraData
import net.xzos.upgradeAll.server.ServerContainer

import org.jetbrains.annotations.Contract
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findAll

object HubManager {

    private val Log = ServerContainer.Log

    private const val TAG = "HubManager"
    private val LogObjectTag = arrayOf("Core", TAG)

    // 读取 hub 数据库
    val databases: List<HubDatabase>
        get() = LitePal.findAll()

    @Contract("null, _ -> false; !null, null -> false")
    fun add(hubConfigGson: HubConfig?, jsCode: String?): Boolean {
        if (hubConfigGson != null && jsCode != null) {
            var name: String? = null
            var uuid: String? = null
            try {
                name = hubConfigGson.info.hubName
                uuid = hubConfigGson.uuid
            } catch (e: NullPointerException) {
                val gson = Gson()
                Log.e(LogObjectTag, TAG, "add: 请确认 hubConfig 包含各个必须元素 hubConfigGson: " + gson.toJson(hubConfigGson))
            }

            // 如果设置了名字与 UUID，则存入数据库
            if (name != null && uuid != null) {
                // 修改数据库
                val hubDatabases: List<HubDatabase> = LitePal.where("uuid = ?", uuid).find()
                val hubDatabaseExtraData = HubDatabaseExtraData(jsCode)
                if (hubDatabases.isNotEmpty()) {
                    val hubDatabase = hubDatabases[0]
                    hubDatabase.name = name
                    hubDatabase.uuid = uuid
                    hubDatabase.hubConfig = hubConfigGson
                    // 存储 js 代码
                    hubDatabase.extraData = hubDatabaseExtraData
                    hubDatabase.save() // 将数据存入 HubDatabase 数据库
                } else
                    HubDatabase(name = name, uuid = uuid, hub_config = Gson().toJson(hubConfigGson), extra_data = Gson().toJson(hubDatabaseExtraData)).save()
                // 开启数据库
                return true
            }
        }
        return false
    }

    fun del(uuid: String) {
        LitePal.deleteAll(HubDatabase::class.java, "uuid = ?", uuid)
    }

    fun getDatabase(uuid: String): HubDatabase? {
        val hubDatabases: List<HubDatabase> = LitePal.where("uuid = ?", uuid).find()
        return if (hubDatabases.isNotEmpty())
            hubDatabases[0]
        else
            null
    }

    fun getJsCode(uuid: String?): String? {
        val hubDatabases: List<HubDatabase> = LitePal.where("uuid = ?", uuid).find()
        return if (hubDatabases.isNotEmpty()) {
            val hubDatabase = hubDatabases[0]
            getJsCodeFromHubDatabaseItem(hubDatabase)
        } else null
    }

    private fun getJsCodeFromHubDatabaseItem(hubDatabase: HubDatabase): String? {
        val extraData: HubDatabaseExtraData = hubDatabase.extraData as HubDatabaseExtraData
        return extraData.javascript
    }
}
