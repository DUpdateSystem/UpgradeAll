package net.xzos.upgradeall.core.module

import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.network.GrpcApi
import net.xzos.upgradeall.core.route.ReleaseListItem
import net.xzos.upgradeall.core.utils.AutoTemplate

class Hub(private val hubDatabase: HubEntity) {
    val name get() = hubDatabase.hubConfig.info.hubName
    val uuid get() = hubDatabase.uuid
    val hubConfig get() = hubDatabase.hubConfig

    fun isValidApp(appId: Map<String, String?>): Boolean = getValidKey(appId) != null

    private fun getValidKey(appId: Map<String, String?>): Map<String, String>? {
        return filterValidKey(appId)
    }

    private fun filterValidKey(appId: Map<String, String?>): Map<String, String>? {
        val apiKeywords = hubDatabase.hubConfig.apiKeywords
        val keyMap = mutableMapOf<String, String>()
        for (key in apiKeywords) {
            keyMap[key] = appId[key] ?: return null
        }
        return keyMap
    }


    fun isIgnoreApp(_appId: Map<String, String?>): Boolean {
        val appId = filterValidKey(_appId) ?: return true
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        return ignoreAppIdList.contains(appId)
    }

    suspend fun ignoreApp(_appId: Map<String, String?>) {
        val appId = filterValidKey(_appId) ?: return
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        ignoreAppIdList.add(appId)
        saveDatabase()
    }

    suspend fun unignoreApp(_appId: Map<String, String?>) {
        val appId = filterValidKey(_appId) ?: return
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        ignoreAppIdList.remove(appId)
        saveDatabase()
    }

    private fun getAppPriority(_appId: Map<String, String?>): Int {
        val appId = filterValidKey(_appId) ?: return LOW_PRIORITY_APP
        return if (isInactiveApp(appId))
            LOW_PRIORITY_APP
        else NORMAL_PRIORITY_APP
    }

    fun isInactiveApp(_appId: Map<String, String?>): Boolean {
        val appId = filterValidKey(_appId) ?: return false
        val inactiveAppIdList = hubDatabase.ignoreAppIdList
        return inactiveAppIdList.contains(appId)
    }

    private suspend fun setInactiveApp(_appId: Map<String, String?>) {
        val appId = filterValidKey(_appId) ?: return
        val lowPriorityAppIdList = hubDatabase.ignoreAppIdList
        lowPriorityAppIdList.add(appId)
        saveDatabase()
    }

    private suspend fun removeInactiveApp(_appId: Map<String, String?>) {
        val appId = filterValidKey(_appId) ?: return
        val lowPriorityAppIdList = hubDatabase.ignoreAppIdList
        lowPriorityAppIdList.remove(appId)
        saveDatabase()
    }

    suspend fun setApplicationsMode(enable: Boolean) {
        hubDatabase.applicationsMode = enable
        saveDatabase()
    }

    fun isEnableApplicationsMode(): Boolean = hubDatabase.applicationsMode

    fun applicationsModeAvailable(): Boolean {
        val apiKeywords = hubDatabase.hubConfig.apiKeywords
        return apiKeywords.contains(ANDROID_APP_TYPE) || apiKeywords.contains(
            ANDROID_MAGISK_MODULE_TYPE
        )
    }

    internal fun getUrl(app: App): String? {
        val appId = app.appId.map {
            "%${it.key}" to it.value
        }.toMap()
        val argsKey = appId.keys
        val appUrlTemplates = hubDatabase.hubConfig.appUrlTemplates
        for (temp in appUrlTemplates) {
            val keywordMatchResultList = AutoTemplate.getArgsKeywords(temp)
            val keywords = keywordMatchResultList.map { it.value }.toList()
            if (argsKey.containsAll(keywords))
                return AutoTemplate.fillArgs(temp, appId)
        }
        return null
    }

    internal suspend fun getAppReleaseList(_appId: Map<String, String?>): List<ReleaseListItem>? {
        val appId = getValidKey(_appId) ?: return null
        return GrpcApi.getAppRelease(uuid, hubDatabase.auth, appId, getAppPriority(appId))
            ?.also {
                if (it.isEmpty())
                    setInactiveApp(appId)
                else
                    removeInactiveApp(appId)
            }
    }

    private suspend fun saveDatabase() {
        HubManager.updateHub(hubDatabase)
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