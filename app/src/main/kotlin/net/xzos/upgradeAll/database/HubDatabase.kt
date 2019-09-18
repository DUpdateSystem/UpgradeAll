package net.xzos.upgradeAll.database

import com.google.gson.Gson
import net.xzos.upgradeAll.json.gson.HubConfig
import net.xzos.upgradeAll.json.gson.HubDatabaseExtraData
import org.litepal.crud.LitePalSupport

open class HubDatabase(
        var name: String,
        var uuid: String,
        private var hub_config: String,
        private var extra_data: String
) : LitePalSupport() {
    val id: Long = 0

    var hubConfig: Any
        set(value) {
            hub_config = Gson().toJson(value)
        }
        get() {
            return Gson().fromJson(hub_config, HubConfig::class.java)
        }
    var extraData: Any?
        set(value) {
            extra_data = Gson().toJson(value)
        }
        get() {
            return Gson().fromJson(extra_data, HubDatabaseExtraData::class.java)
        }
}