package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.utils.*
import net.xzos.upgradeall.core.utils.android_app.getAppVersion

class Updater internal constructor(
    private val app: App, private val versionUtils: VersionUtils,
    internal var statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {},
) {

    private var tmpUpdateStatus: Int = NETWORK_ERROR - 1

    internal fun getUpdateStatus(): Int {
        val versionNumberList = app.versionList
        val status = if (versionNumberList.isEmpty()) {
            NETWORK_ERROR
        } else {
            val installedVersionNumber = getInstalledVersionNumber()
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

    private fun getIgnoreVersionNumber(): String? = app.appDatabase.ignoreVersionNumber

    /* App 在本地的版本号 */
    internal fun getRawInstalledVersionStringList(): List<Pair<Char, Boolean>>? {
        return versionUtils.getKeyVersionNumber(
            getAppVersion(app.appId) ?: return null
        )
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