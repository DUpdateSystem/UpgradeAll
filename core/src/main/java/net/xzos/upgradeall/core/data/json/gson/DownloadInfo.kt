package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

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

fun getJsonMap(json:String):Map<String,String>{
    val type = object : TypeToken<Map<String, String>>() {}.type
    return Gson().fromJson(json, type)
}