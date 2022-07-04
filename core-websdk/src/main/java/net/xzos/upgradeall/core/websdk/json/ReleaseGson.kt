package net.xzos.upgradeall.core.websdk.json

import com.google.gson.annotations.SerializedName

data class ReleaseGson(
    @SerializedName("version_number") val versionNumber: String,
    @SerializedName("change_log") val changelog: String?,
    @SerializedName("assets") val assetGsonList: List<AssetGson>,
    @SerializedName("extra") val extra: Map<String, Any?>? = mapOf(),
)

data class AssetGson(
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_type") val fileType: String?,
    @SerializedName("download_url") val downloadUrl: String?,
)