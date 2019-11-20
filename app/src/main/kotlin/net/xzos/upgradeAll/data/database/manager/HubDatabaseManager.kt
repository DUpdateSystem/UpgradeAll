package net.xzos.upgradeAll.data.database.manager

import com.google.gson.Gson
import net.xzos.upgradeAll.data.database.litepal.HubDatabase
import net.xzos.upgradeAll.data.json.gson.HubConfig
import net.xzos.upgradeAll.data.json.gson.HubDatabaseExtraData
import net.xzos.upgradeAll.server.ServerContainer
import org.litepal.LitePal
import org.litepal.extension.find
import org.litepal.extension.findAll

object HubDatabaseManager {

    private val Log = ServerContainer.Log

    private const val TAG = "HubDatabaseManager"
    private val LogObjectTag = arrayOf("Core", TAG)

    // 读取 hub 数据库
    val databases: List<HubDatabase>
        get() = LitePal.findAll()

    fun addDatabase(hubConfigGson: HubConfig, jsCode: String): Boolean {
        val name: String? = hubConfigGson.info.hubName
        val uuid: String? = hubConfigGson.uuid

        // 如果设置了名字与 UUID，则存入数据库
        if (name != null && uuid != null) {
            val hubDatabaseExtraData = HubDatabaseExtraData(jsCode)
            // 修改数据库
            getDatabase(uuid)?.let {
                it.name = name
                it.uuid = uuid
                it.hubConfig = hubConfigGson
                // 存储 js 代码
                it.extraData = hubDatabaseExtraData
                it.save() // 将数据存入 HubDatabase 数据库
            } ?: HubDatabase(
                    name = name,
                    uuid = uuid,
                    hub_config = Gson().toJson(hubConfigGson),
                    extra_data = Gson().toJson(hubDatabaseExtraData)
            ).save()
            ServerContainer.AppManager.renewAppInHub(uuid)  // 更新相关跟踪项
            return true
        }
        return false
    }

    fun del(uuid: String) {
        LitePal.deleteAll(HubDatabase::class.java, "uuid = ?", uuid)
    }

    fun exists(uuid: String?): Boolean {
        return LitePal.where("uuid = ?", uuid).find<HubDatabase>().isNotEmpty()
    }

    fun getDatabase(uuid: String?): HubDatabase? {
        val hubDatabases: List<HubDatabase> = LitePal.where("uuid = ?", uuid).find()
        return if (hubDatabases.isNotEmpty())
            hubDatabases[0]
        else
            null
    }

    fun getJsCode(uuid: String?): String? {
        return getDatabase(uuid)?.extraData?.javascript
    }
}