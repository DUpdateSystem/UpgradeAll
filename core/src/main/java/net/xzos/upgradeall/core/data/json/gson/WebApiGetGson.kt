package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.annotations.SerializedName

class WebApiGetGson {
    @SerializedName("app_info_list")
    val appInfoList: MutableList<List<AppInfoListBean>> = mutableListOf()

    /**
     * key : android_app_package
     * value : com.aurora.adroid
     */
    class AppInfoListBean(
        var key: String,
        var value: String
    ) {
        override fun toString(): String {
            return "{key: $key, value: $value}"
        }
    }
}

fun List<WebApiGetGson.AppInfoListBean>.key(hubUuid: String): String? {
    if (this.isEmpty()) return null
    var key = hubUuid
    for (i in this) {
        key += "+${i.value}"
    }
    return key
}
