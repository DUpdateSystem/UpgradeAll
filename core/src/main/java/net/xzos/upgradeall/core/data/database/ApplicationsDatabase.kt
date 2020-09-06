package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.json.gson.IgnoreApp

class ApplicationsDatabase(
        id: Long,
        name: String,
        hubUuid: String,
        auth: Map<String, String?> = mapOf(),
        extraId: Map<String, String?> = mapOf(),
        var invalidPackageList: MutableList<Map<String, String?>> = mutableListOf(),
        var ignoreApps: MutableList<IgnoreApp> = mutableListOf()
) : BaseAppDatabase(id, name, hubUuid, extraId, auth) {
    fun getIgnoreVersionNumber(appId: Map<String, String?>): String? {
        for (app in ignoreApps) {
            if (app.packageId == appId)
                return app.versionNumber
        }
        return null
    }

    fun removeIgnore(appId: Map<String, String?>) {
        var app: IgnoreApp? = null
        for (app1 in ignoreApps) {
            if (app1.packageId == appId) {
                app = app1
                break
            }
        }
        if (app != null) ignoreApps.remove(app)
    }
}
