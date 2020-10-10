package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.annotations.SerializedName

data class DownloadInfoItem(
        @SerializedName("name") val name: String,
        @SerializedName("url") val url: String,
        @SerializedName("headers") val headers: Map<String, String>,
        @SerializedName("cookies") val cookies: Map<String, String>,
)