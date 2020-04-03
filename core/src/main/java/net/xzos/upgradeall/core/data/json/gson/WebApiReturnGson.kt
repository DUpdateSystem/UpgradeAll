package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class WebApiReturnGson(
        @SerializedName("app_info")
        var appInfo: List<WebApiGetGson.AppInfoListBean> = listOf(),
        @SerializedName("release_info")
        var releaseInfo: List<ReleaseInfoBean>? = null
) {
    /**
     * version_number : 1.0.4
     * change_log:
     * assets : [{"name":"com.aurora.adroid_4.apk","download_url":"https://f-droid.org/repo/com.aurora.adroid_4.apk"}]
     */
    class ReleaseInfoBean(
            @SerializedName("version_number")
            var versionNumber: String,
            @SerializedName("change_log")
            var changeLog: String?,

            @SerializedName("assets")
            var assets: List<AssetsBean> = listOf()
    ) {
        /**
         * name : com.aurora.adroid_4.apk
         * download_url : https://f-droid.org/repo/com.aurora.adroid_4.apk
         */
        class AssetsBean(
                @SerializedName("name")
                var name: String,

                @SerializedName("download_url")
                var downloadUrl: String

        )

        override fun toString(): String {
            return Gson().toJson(this)
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}