package net.xzos.upgradeAll.database

import com.google.gson.Gson
import net.xzos.upgradeAll.json.gson.HubConfig
import net.xzos.upgradeAll.json.gson.HubDatabaseExtraData
import org.litepal.crud.LitePalSupport

open class HubDatabase(
        var name: String,
        var uuid: String,
        private var hub_config: String?,
        private var extra_data: String?
) : LitePalSupport() {
    val id: Long = 0

    var hubConfig: Any?
        set(value) {
            if (value != null && value is HubConfig)
                hub_config = Gson().toJson(value)
        }
        get() {
            return if (hub_config != null) Gson().fromJson(hub_config, HubConfig::class.java)
            else null
        }
    var extraData: Any?
        set(value) {
            if (value != null && value is HubDatabaseExtraData)
                extra_data = Gson().toJson(value)
        }
        get() {
            return if (extra_data != null) Gson().fromJson(extra_data, HubDatabaseExtraData::class.java)
            else null
        }
}