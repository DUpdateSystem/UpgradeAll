package net.xzos.upgradeall.data_manager.database.litepal

import com.google.gson.Gson
import net.xzos.dupdatesystem.data.json.gson.HubConfig
import net.xzos.dupdatesystem.data.json.gson.HubDatabaseExtraData
import org.litepal.crud.LitePalSupport


internal class HubDatabase(
        var name: String,
        var uuid: String,
        private var hub_config: String?,
        private var extra_data: String?
) : LitePalSupport() {
    val id: Long = 0

    var cloudHubConfig: HubConfig?
        set(value) {
            if (value != null)
                hub_config = Gson().toJson(value)
        }
        get() {
            return if (hub_config != null)
                Gson().fromJson(hub_config, HubConfig::class.java)
            else null
        }
    var extraData: HubDatabaseExtraData?
        set(value) {
            if (value != null)
                extra_data = Gson().toJson(value)
        }
        get() {
            return if (extra_data != null)
                Gson().fromJson(extra_data, HubDatabaseExtraData::class.java)
            else null
        }

    override fun save(): Boolean {
        return if (name.isNotBlank() && uuid.isNotBlank()
                && !hub_config.isNullOrBlank() && !extra_data.isNullOrBlank()) {
            super.save()
        } else false
    }

}
