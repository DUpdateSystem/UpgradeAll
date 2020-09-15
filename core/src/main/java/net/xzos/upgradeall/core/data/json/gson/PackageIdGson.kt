package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * api :
 * text :
 */
class PackageIdGson(
    var api: String? = null,
    @SerializedName("extra_string")var extraString: String? = null
){
    companion object {
        @Transient
        const val API_TYPE_APP_PACKAGE = "app_package"
        @Transient
        const val API_TYPE_MAGISK_MODULE = "magisk_module"
        @Transient
        const val API_TYPE_SHELL = "shell"
        @Transient
        const val API_TYPE_SHELL_ROOT = "shell_root"
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}