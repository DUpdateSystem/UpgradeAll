package net.xzos.upgradeAll.data.json.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AppDatabaseExtraData(
        @SerializedName("cloud_app_config") private var cloud_app_config: String? = null,  // TODO: 尝试类型优化
        @SerializedName("mark_processed_version_number") internal var markProcessedVersionNumber: String? = null
) {
    var cloudAppConfig: AppConfig? = null
        get() =
            if (cloud_app_config != null)
                Gson().fromJson(cloud_app_config, AppConfig::class.java)
            else null
    set(value) {
        field = value
        cloud_app_config = Gson().toJson(value)
    }
}
