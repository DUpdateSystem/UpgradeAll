package net.xzos.upgradeall.core.module

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.database.table.HubEntity
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.data.DataStorage
import net.xzos.upgradeall.core.utils.AutoTemplate
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.constant.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class Hub(private val hubDatabase: HubEntity) {
    val name get() = hubDatabase.hubConfig.info.hubName
    val uuid get() = hubDatabase.uuid
    val auth get() = hubDatabase.auth
    val hubConfig get() = hubDatabase.hubConfig

    fun isValidApp(appId: Map<String, String?>): Boolean = getValidKey(appId) != null

    fun getValidKey(appId: Map<String, String?>): Map<String, String?>? {
        return filterValidKey(appId).first.ifEmpty { null }
    }

    fun filterValidKey(appId: Map<String, String?>): Pair<Map<String, String?>, Map<String, String?>> {
        val mapPair = Pair(mutableMapOf<String, String?>(), mutableMapOf<String, String?>())
        val apiKeywords = hubDatabase.hubConfig.apiKeywords
        appId.forEach {
            if (it.key in apiKeywords) {
                mapPair.first[it.key] = it.value
            } else {
                mapPair.second[it.key] = it.value
            }
        }
        return mapPair
    }

    fun isIgnoreApp(_appId: Map<String, String?>): Boolean {
        val appId = getValidKey(_appId) ?: return true
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        return ignoreAppIdList.contains(appId)
    }

    suspend fun ignoreApp(_appId: Map<String, String?>) {
        val appId = getValidKey(_appId) ?: return
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        ignoreAppIdList.add(appId)
        saveDatabase()
    }

    suspend fun unignoreApp(_appId: Map<String, String?>) {
        val appId = getValidKey(_appId) ?: return
        val ignoreAppIdList = hubDatabase.userIgnoreAppIdList
        ignoreAppIdList.remove(appId)
        saveDatabase()
    }

    private fun getAppPriority(_appId: Map<String, String?>): Int {
        val appId = getValidKey(_appId) ?: return LOW_PRIORITY_APP
        return if (isActiveApp(appId))
            NORMAL_PRIORITY_APP
        else
            LOW_PRIORITY_APP
    }

    fun isActiveApp(_appId: Map<String, String?>): Boolean {
        val appId = getValidKey(_appId) ?: return false
        val inactiveAppIdList = hubDatabase.ignoreAppIdList
        return !inactiveAppIdList.contains(appId)
    }

    private suspend fun setActiveApp(_appId: Map<String, String?>) {
        val appId = getValidKey(_appId) ?: return
        val lowPriorityAppIdList = hubDatabase.ignoreAppIdList
        if (lowPriorityAppIdList.remove(appId))
            saveDatabase()
    }

    private suspend fun unsetActiveApp(_appId: Map<String, String?>) {
        val appId = getValidKey(_appId) ?: return
        val lowPriorityAppIdList = hubDatabase.ignoreAppIdList
        if (lowPriorityAppIdList.add(appId))
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

    internal fun getAppReleaseList(
        appDataStorage: DataStorage, callback: (List<ReleaseGson>?) -> Unit
    ) {
        val (appId, other) = filterValidKey(appDataStorage.appDatabase.appId)
        if (appId.isEmpty()) {
            callback(null)
            return
        }
        appDataStorage.serverApi.getAppReleaseList(ApiRequestData(uuid, auth, appId, other)) {
            it?.let {
                runBlocking {
                    if (it.isEmpty())
                        unsetActiveApp(appId)
                    else
                        setActiveApp(appId)
                }
            }
            callback(it)
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