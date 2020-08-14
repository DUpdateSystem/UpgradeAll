package net.xzos.upgradeall.data.database

import com.google.gson.Gson
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import org.json.JSONObject
import org.litepal.crud.LitePalSupport
import java.security.MessageDigest


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

    fun parseFromJson(json: JSONObject) {
        uuid = json["uuid"] as String
        hub_config = json["hub_config"] as String
    }

    fun toJson(): JSONObject {
        return JSONObject(mapOf(
                "uuid" to uuid,
                "hub_config" to hub_config
        ))
    }

    fun md5(): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(uuid.toByteArray())
        return md.digest().toString()
    }
}
