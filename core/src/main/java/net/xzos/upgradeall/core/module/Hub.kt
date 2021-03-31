package net.xzos.upgradeall.core.module

import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.network.GrpcApi
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.utils.AutoTemplate

class Hub(private val hubDatabase: HubEntity) {
    val name get() = hubDatabase.hubConfig.info.hubName
    val uuid get() = hubDatabase.uuid
    val hubConfig get() = hubDatabase.hubConfig

    fun isValidApp(app: App): Boolean = getValidKey(app) != null

    private fun getValidKey(app: App): Map<String, String>? {
        val apiKeywords = hubDatabase.hubConfig.apiKeywords
        val appId = app.appId
        val keyMap = mutableMapOf<String, String>()
        for (key in apiKeywords) {
            keyMap[key] = appId[key] ?: return null
        }
        return keyMap
    }

    fun isIgnoreApp(app: App): Boolean {
        val appId = getValidKey(app) ?: return true
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        return ignoreAppIdList.contains(appId)
    }

    fun ignoreApp(app: App) {
        val appId = getValidKey(app) ?: return
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        ignoreAppIdList.add(appId)
    }

    fun unignoreApp(app: App) {
        val appId = getValidKey(app) ?: return
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        ignoreAppIdList.remove(appId)
    }

    private fun getAppPriority(app: App): Int {
        val appId = getValidKey(app) ?: return LOW_PRIORITY_APP
        val lowPriorityAppIdList = hubDatabase.ignoreAppIdList
        return if (lowPriorityAppIdList.contains(appId))
            LOW_PRIORITY_APP
        else NORMAL_PRIORITY_APP
    }

    private fun setLowPriorityApp(app: App) {
        val appId = getValidKey(app) ?: return
        val lowPriorityAppIdList = hubDatabase.ignoreAppIdList
        lowPriorityAppIdList.add(appId)
    }

    private fun unsetLowPriorityApp(app: App) {
        val appId = getValidKey(app) ?: return
        val lowPriorityAppIdList = hubDatabase.ignoreAppIdList
        lowPriorityAppIdList.remove(appId)
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
        val appId = getValidKey(app) ?: return null
        return GrpcApi.getAppRelease(uuid, hubDatabase.auth, appId, getAppPriority(app))?.also {
            if (it.isEmpty())
                setLowPriorityApp(app)
            else
                unsetLowPriorityApp(app)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is Hub && hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    companion object {
        private const val LOW_PRIORITY_APP = -1
        private const val NORMAL_PRIORITY_APP = 0
    }
}