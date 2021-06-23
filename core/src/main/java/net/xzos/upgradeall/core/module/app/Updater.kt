package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.module.app.data.DataGetter
import net.xzos.upgradeall.core.module.app.data.DataStorage
import net.xzos.upgradeall.core.module.app.version.VersionUtils
import net.xzos.upgradeall.core.utils.*
import net.xzos.upgradeall.core.utils.android_app.getAppVersion

class Updater internal constructor(
    private val dataStorage: DataStorage,
    internal var statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {},
) {

    private val dataGetter = DataGetter(dataStorage)
    private var tmpUpdateStatus: Int = NETWORK_ERROR - 1

    internal fun getReleaseStatus(): Int {
        val versionNumberList = dataStorage.versionData.getVersionList()
        val status = if (versionNumberList.isEmpty()) {
            NETWORK_ERROR
        } else {
            val installedVersionNumber = getInstalledVersionNumber()
                ?: dataStorage.appDatabase.ignoreVersionNumber
            when {
                versionNumberList.first().isIgnored -> APP_LATEST
                installedVersionNumber == null -> APP_NO_LOCAL
                !isLatestVersionNumber(
                    installedVersionNumber,
                    versionNumberList.filter { !it.isIgnored }.map { it.name }
                ) -> APP_OUTDATED
                else -> APP_LATEST
            }
        }
        if (status != tmpUpdateStatus) {
            statusRenewedFun(status)
            tmpUpdateStatus = status
        }
        return status
    }

    /* App 在本地的版本号 */
    internal fun getRawInstalledVersionStringList(): List<Pair<Char, Boolean>>? {
        val appDatabase = dataStorage.appDatabase
        return VersionUtils.getKeyVersionNumber(
            getAppVersion(appDatabase.appId) ?: return null,
            appDatabase.invalidVersionNumberFieldRegexString
        )
    }

    /* 刷新版本号数据 */
    suspend fun update() {
        dataGetter.update()
    }

    suspend fun getReleaseStatusWaitRenew(): Int {
        dataStorage.renewMutex.wait()
        return getReleaseStatus()
    }

    internal fun getInstalledVersionNumber(): String? {
        return VersionUtils.getKey(getRawInstalledVersionStringList() ?: return null)
    }

    private fun isLatestVersionNumber(
        localVersionNumber: String,
        rowVersionNumberList: List<String>
    ): Boolean {
        val versionNumberList = rowVersionNumberList.filter { it.isNotBlank() }
        if (versionNumberList.isEmpty()) return true
        val latestVersion = versionNumberList[0]
        return when (VersioningUtils.compareVersionNumber(localVersionNumber, latestVersion)) {
            1 -> true
            0 -> true
            -1 -> false
            else -> {
                val searchUtils = SearchUtils(versionNumberList.map {
                    SearchInfo(MatchInfo(it, it, listOf(MatchString(it))), null)
                })
                val searchList = searchUtils.search(localVersionNumber)
                if (searchList.isNotEmpty()) {
                    val version = searchList[0].matchInfo.matchList[0].matchString
                    versionNumberList.indexOf(version) == 0
                } else false
            }
        }
    }

    companion object {
        const val NETWORK_ERROR = 0
        const val APP_LATEST = 1
        const val APP_OUTDATED = 2
        const val APP_NO_LOCAL = 3
    }
}