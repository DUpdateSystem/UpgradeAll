package net.xzos.upgradeall.core.module

import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.network.GrpcApi
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.utils.AutoTemplate

class Hub(private val hubDatabase: HubEntity) {
    val name get() = hubDatabase.hubConfig.info.hubName
    val uuid get() = hubDatabase.uuid
    fun isValidApp(app: App): Boolean {
        val appId = getValidKey(app) ?: return false
        for (ignoreAppId in hubDatabase.ignoreAppIdList) {
            if (ignoreAppId == appId) {
                return true
            }
        }
        return false
    }

    private fun getValidKey(app: App): Map<String, String>? {
        val apiKeywords = hubDatabase.hubConfig.apiKeywords
        val appId = app.appId
        val keyMap = mutableMapOf<String, String>()
        for (key in apiKeywords) {
            keyMap[key] = appId[key] ?: return null
        }
        return keyMap
    }

    fun ignoreApp(app: App) {
        val appId = getValidKey(app) ?: return
        val ignoreAppIdList = hubDatabase.ignoreAppIdList
        ignoreAppIdList.add(appId)
    }

    fun unignoreApp(app: App) {
        val appId = getValidKey(app) ?: return
        val ignoreAppIdList = hubDatabase.ignoreAppIdList
        ignoreAppIdList.remove(appId)
    }

    internal fun getUrl(app: App): String? {
        val appId = app.appId
        val argsKey = appId.keys
        val appUrlTemplates = hubDatabase.hubConfig.appUrlTemplates
        for (temp in appUrlTemplates) {
            val keywordMatchResultList = AutoTemplate.getArgsKeywords(temp)
            val keywords = keywordMatchResultList.map { it.value }
            if (keywords == argsKey)
                return AutoTemplate.fillArgs(temp, appId)
        }
        return null
    }

    internal suspend fun getAppReleaseList(app: App): List<ReleaseListItem>? {
        return GrpcApi.getAppRelease(uuid, hubDatabase.auth, app.appId)
    }

    override fun equals(other: Any?): Boolean {
        return other is Hub && hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}