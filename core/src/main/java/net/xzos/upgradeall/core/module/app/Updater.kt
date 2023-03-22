package net.xzos.upgradeall.core.module.app

import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.androidutils.app_info.getAppVersion
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.app.data.DataGetter
import net.xzos.upgradeall.core.module.app.data.DataStorage
import net.xzos.upgradeall.core.module.app.version.VersionEntityUtils
import net.xzos.upgradeall.core.module.app.version.VersionInfo
import net.xzos.upgradeall.core.utils.MatchInfo
import net.xzos.upgradeall.core.utils.MatchString
import net.xzos.upgradeall.core.utils.SearchInfo
import net.xzos.upgradeall.core.utils.SearchUtils

class Updater internal constructor(
    private val dataStorage: DataStorage,
    private val versionEntityUtils: VersionEntityUtils,
    internal var statusRenewedFun: (appStatus: AppStatus) -> Unit = fun(_) {},
) {

    private val dataGetter = DataGetter(dataStorage)
    private var tmpUpdateStatus: AppStatus? = null

    internal fun getReleaseStatus(): AppStatus {
        val versionNumberList = dataStorage.versionMap.getVersionList()
        val status = if (versionNumberList.isEmpty()) {
            AppStatus.NETWORK_ERROR
        } else {
            val localVersion = getLocalVersion()
                ?: dataStorage.appDatabase.getIgnoreVersion()
            when {
                versionEntityUtils.isIgnored(versionNumberList.first().versionInfo.name) -> AppStatus.APP_LATEST
                localVersion == null -> AppStatus.APP_NO_LOCAL
                !isLatestVersionNumber(
                    localVersion, versionNumberList.map { it.versionInfo }
                ) -> AppStatus.APP_OUTDATED
                else -> AppStatus.APP_LATEST
            }
        }
        if (status != tmpUpdateStatus) {
            statusRenewedFun(status)
            tmpUpdateStatus = status
        }
        return status
    }

    /* App 在本地的版本号 */
    internal fun getLocalVersion(): VersionInfo? {
        val appDatabase = dataStorage.appDatabase
        val appVersionInfo = getAppVersion(appDatabase.appId) ?: return null
        return VersionInfo.new(
            appVersionInfo.name,
            appDatabase.invalidVersionNumberFieldRegexString,
            appDatabase.includeVersionNumberFieldRegexString,
            appVersionInfo.extra,
        )
    }

    /* 刷新版本号数据 */
    suspend fun update() {
        dataGetter.update()
    }

    suspend fun getReleaseStatusWaitRenew(): AppStatus {
        return dataStorage.renewMutex.withLock {
            getReleaseStatus()
        }
    }

    private fun isLatestVersionNumber(
        localVersion: VersionInfo,
        versionList: List<VersionInfo>
    ): Boolean {
        if (versionList.isEmpty()) return true
        val latestVersion = versionList[0]
        return when (localVersion.compareToOrError(latestVersion)) {
            1 -> true
            0 -> true
            -1 -> false
            else -> {
                val versionNumberList = versionList.map { it.name }
                val localVersionName = localVersion.name
                val searchUtils = SearchUtils(versionNumberList.map {
                    SearchInfo(MatchInfo(it, it, listOf(MatchString(it))), null)
                })
                val searchList = searchUtils.search(localVersionName)
                if (searchList.isNotEmpty()) {
                    val version = searchList[0].matchInfo.matchList[0].matchString
                    versionNumberList.indexOf(version) == 0
                } else false
            }
        }
    }

    fun isRenewing(): Boolean {
        return dataStorage.renewMutex.isLocked
    }
}