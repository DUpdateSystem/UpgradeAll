package net.xzos.upgradeall.websdk.data.json

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * base_version: 2
 * config_version: 1
 * uuid:
 * base_hub_uuid:
 * info: {"name": "", "desc": "", "url": , "extra_map": ""}
 */
data class AppConfigGson(
    @SerializedName("base_version")
    @JsonProperty("base_version")
    val baseVersion: Int,
    @SerializedName("config_version")
    @JsonProperty("config_version")
    val configVersion: Int,
    @SerializedName("uuid")
    @JsonProperty("uuid")
    val uuid: String,
    @SerializedName("base_hub_uuid")
    @JsonProperty("base_hub_uuid")
    val baseHubUuid: String,
    @SerializedName("info")
    @JsonProperty("info")
    val info: InfoBean,
) {

    /**
     * app_name:
     * url:
     * desc:
     */
    data class InfoBean(
        @SerializedName("name")
        @JsonProperty("name")
        val name: String,
        @SerializedName("url")
        @JsonProperty("url")
        var url: String,
        @SerializedName("desc")
        @JsonProperty("desc")
        var desc: String?,
        @SerializedName("extra_map")
        @JsonProperty("extra_map")
        var extraMap: Map<String, String>,
    )

    override fun toString(): String {
        return Gson().toJson(this)
    }
}