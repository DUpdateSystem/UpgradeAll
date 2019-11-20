package net.xzos.upgradeAll.data.json.gson

import com.google.gson.annotations.SerializedName

data class AppDatabaseExtraData(
        @SerializedName("cloud_app_config") var cloudAppConfig: String? = null  // TODO: 尝试类型优化
)
