package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.data.coroutines.coroutinesMutableListOf
import net.xzos.upgradeall.core.data.json.gson.IgnoreApp

class ApplicationsDatabase(
        id: Long,
        name: String,
        hubUuid: String,
        auth: Map<String, String?> = mapOf(),
        extraId: Map<String, String?> = mapOf(),
        val invalidPackageList: CoroutinesMutableList<Map<String, String?>> = coroutinesMutableListOf(true),
        val ignoreApps: CoroutinesMutableList<IgnoreApp> = coroutinesMutableListOf(true)
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

    fun addIgnore(appId: Map<String, String?>, versionNumber: String) {
        for (app in ignoreApps) {
            if (app.packageId == appId) {
                app.versionNumber = versionNumber
                return
            }
        }
        ignoreApps.add(IgnoreApp.getInstance(appId, versionNumber))
    }
}
