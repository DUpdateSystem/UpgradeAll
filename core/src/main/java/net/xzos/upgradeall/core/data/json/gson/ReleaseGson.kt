package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.annotations.SerializedName

data class ReleaseGson(
    @SerializedName("version_number") val versionNumber: String,
    @SerializedName("change_log") val changelog: String,
    @SerializedName("assets") val assetList: List<Assets>,
)

data class Assets(
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_type") val fileType: String?,
    @SerializedName("download_url") val downloadUrl: String?,
)