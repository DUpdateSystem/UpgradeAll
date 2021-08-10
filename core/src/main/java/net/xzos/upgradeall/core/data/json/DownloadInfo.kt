package net.xzos.upgradeall.core.data.json

import com.google.gson.annotations.SerializedName
import net.xzos.upgradeall.core.utils.getJsonMap

data class DownloadItem(
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("headers") val _headers: String?,
    @SerializedName("cookies") val _cookies: String?,
) {
    fun getHeaders(): Map<String, String> {
        return getJsonMap(_headers ?: return mapOf())
    }

    fun getCookies(): Map<String, String> {
        return getJsonMap(_cookies ?: return mapOf())
    }
}