package net.xzos.upgradeall.data.database

import com.google.gson.Gson
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import org.litepal.crud.LitePalSupport


internal class HubDatabase(
        var uuid: String,
        private var hub_config: String?
) : LitePalSupport() {
    val id: Long = 0

    var hubConfig: HubConfigGson
        set(value) {
            hub_config = Gson().toJson(value)
        }
        get() {
            return if (hub_config != null)
                Gson().fromJson(hub_config, HubConfigGson::class.java)
            else HubConfigGson()
        }

    override fun save(): Boolean {
        return if (uuid.isNotBlank()
                && !hub_config.isNullOrBlank()) {
            super.save()
        } else false
    }
}
