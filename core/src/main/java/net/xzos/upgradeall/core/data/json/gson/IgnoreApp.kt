package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.annotations.SerializedName
import org.json.JSONObject


class IgnoreApp private constructor(
        @SerializedName("package_id") private var packageIdS: String? = null,
        @SerializedName("version_number") var versionNumber: String? = null
) {
    var packageId: Map<String, String?>
        get() {
            val s = packageIdS ?: return mapOf()
            val json = JSONObject(s)
            val map = mutableMapOf<String, String>()
            for (k in json.keys()) {
                map[k] = json.getString(k)
            }
            return map
        }
        private set(value) {
            val json = JSONObject()
            for ((k, v) in value) {
                json.put(k, v)
            }
            packageIdS = json.toString()
        }

    companion object {
        fun getInstance(packageId: Map<String, String?>, versionNumber: String?) = IgnoreApp().also {
            it.packageId = packageId
            it.versionNumber = versionNumber
        }
    }
}
