package net.xzos.upgradeall.websdk.data.json

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class ReleaseGson(
    @SerializedName("version_number")
    @JsonProperty("version_number")
    val versionNumber: String,
    @SerializedName("changelog")
    @JsonProperty("changelog")
    val changelog: String?,
    @SerializedName("assets")
    @JsonProperty("assets")
    val assetGsonList: List<AssetGson>,
    @SerializedName("extra")
    @JsonProperty("extra")
    val extra: Map<String, Any?>? = mapOf(),
)

data class AssetGson(
    @SerializedName("file_name")
    @JsonProperty("file_name")
    val fileName: String,
    @SerializedName("file_type")
    @JsonProperty("file_type")
    val fileType: String?,
    @SerializedName("download_url")
    @JsonProperty("download_url")
    val downloadUrl: String?,
)