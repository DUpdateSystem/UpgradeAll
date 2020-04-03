package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AppDatabaseExtraData(
    @SerializedName("cloud_app_config") private var cloud_app_config: String? = null,  // TODO: 尝试类型优化
    @SerializedName("mark_processed_version_number") var markProcessedVersionNumber: String? = null,
    @SerializedName("applications_auto_exclude") var applicationsAutoExclude: MutableList<String>? = null
) {
    var cloudAppConfigGson: AppConfigGson? = null
        get() =
            if (cloud_app_config != null)
                Gson().fromJson(cloud_app_config, AppConfigGson::class.java)
            else null
        set(value) {
            val v = value?.copy()
            if (v != null) {
                field = v
                cloud_app_config = Gson().toJson(v)
            }
        }
}
