package net.xzos.upgradeall.websdk.data.json

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class DownloadItem(
    @SerializedName("name")
    @JsonProperty("name")
    val name: String?,
    @SerializedName("url")
    @JsonProperty("url")
    val url: String,
    @SerializedName("headers")
    @JsonProperty("headers")
    val headers: Map<String, String>?,
    @SerializedName("cookies")
    @JsonProperty("cookies")
    val cookies: Map<String, String>?,
)