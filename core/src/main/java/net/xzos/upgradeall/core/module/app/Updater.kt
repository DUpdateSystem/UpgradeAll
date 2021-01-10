package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.utils.*

class Updater internal constructor(
        private val app: App,
        internal var statusRenewedFun: (appStatus: Int) -> Unit = fun(_: Int) {},
) {

    private var tmpUpdateStatus: Int = NETWORK_ERROR - 1

    fun getUpdateStatus(): Int {
        val releaseList = app.versionList
        val status = if (releaseList.isEmpty()) {
            NETWORK_ERROR
        } else {
            val versionNumber = getIgnoreVersionNumber() ?: getInstalledVersionNumber()
            when {
                versionNumber == null -> APP_NO_LOCAL
                !isLatestVersionNumber(versionNumber, releaseList.map { it.name }) -> APP_OUTDATED
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
    private fun getInstalledVersionNumber(): String? = getAppVersion(app.appId)

    private fun isLatestVersionNumber(localVersionNumber: String, versionNumberList: List<String>): Boolean {
        if (versionNumberList.isEmpty()) return true
        val latestVersion = versionNumberList[0]
        return VersioningUtils.compareVersionNumber(
                localVersionNumber, latestVersion
        ) ?: run {
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

    companion object {
        const val NETWORK_ERROR = 0
        const val APP_LATEST = 1
        const val APP_OUTDATED = 2
        const val APP_NO_LOCAL = 3
    }
}