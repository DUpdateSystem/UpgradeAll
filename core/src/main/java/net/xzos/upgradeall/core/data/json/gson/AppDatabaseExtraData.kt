package net.xzos.upgradeall.core.data.json.gson

import com.google.gson.annotations.SerializedName

class AppDatabaseExtraData {
    @SerializedName("mark_processed_version_number")
    var markProcessedVersionNumber: String? = null

    @SerializedName("applications_config")
    var applicationsConfig: ApplicationsConfig? = null
        get() = field ?: ApplicationsConfig()
                .also { field = it }

    @SerializedName("cloud_app_config")
    var cloudAppConfig: AppConfigGson? = null
        get() = field ?: AppConfigGson()
                .also { field = it }

    class ApplicationsConfig {
        @SerializedName("invalid_package_name")
        var invalidPackageName: MutableList<String>? = null
            get() = field ?: mutableListOf<String>()
                    .also { field = it }

        @SerializedName("ignore_apps")
        var ignoreAppList: MutableList<IgnoreApp>? = null
            get() = field ?: mutableListOf<IgnoreApp>()
                    .also { field = it }

        class IgnoreApp(
                @SerializedName("package_name") val packageName: String,
                @SerializedName("forever") var forever: Boolean = false,
                @SerializedName("version_number") var versionNumber: String? = null
        )
    }
}

fun AppDatabaseExtraData.getIgnoreApp(packageName: String?)
        : AppDatabaseExtraData.ApplicationsConfig.IgnoreApp? {
    val ignoreAppList = this.applicationsConfig!!.ignoreAppList!!
    for (ignoreApp in ignoreAppList) {
        if (ignoreApp.packageName == packageName) {
            return ignoreApp
        }
    }
    return null
}

fun AppDatabaseExtraData.addIgnoreAppList(packageName: String, forever: Boolean = false, versionNumber: String?) {
    if (!forever && versionNumber == null) return
    this.getIgnoreApp(packageName)?.also {
        it.versionNumber = versionNumber
    } ?: AppDatabaseExtraData.ApplicationsConfig.IgnoreApp(
            packageName, forever, versionNumber
    ).let {
        val ignoreAppList = this.applicationsConfig!!.ignoreAppList!!
        ignoreAppList.add(it)
    }
}

fun AppDatabaseExtraData.removeIgnoreAppList(packageName: String) {
    val ignoreAppList = this.applicationsConfig!!.ignoreAppList!!
    ignoreAppList.remove(this.getIgnoreApp(packageName))
}
