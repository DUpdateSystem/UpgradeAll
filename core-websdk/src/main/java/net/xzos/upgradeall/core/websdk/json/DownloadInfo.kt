package net.xzos.upgradeall.core.websdk.json

import com.google.gson.annotations.SerializedName

data class DownloadItem(
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String,
    @SerializedName("headers") val headers: Map<String, String>?,
    @SerializedName("cookies") val cookies: Map<String, String>?,
)