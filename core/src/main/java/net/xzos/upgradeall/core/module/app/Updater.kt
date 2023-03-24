package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.app.version.VersionEntityUtils
import net.xzos.upgradeall.core.module.app.version.VersionInfo
import net.xzos.upgradeall.core.utils.MatchInfo
import net.xzos.upgradeall.core.utils.MatchString
import net.xzos.upgradeall.core.utils.SearchInfo
import net.xzos.upgradeall.core.utils.SearchUtils

internal object Updater {

    internal fun getReleaseStatus(app: App): AppStatus {
        val versionNumberList = app.versionList
        val status = if (versionNumberList.isEmpty()) {
            AppStatus.NETWORK_ERROR
        } else {
            val localVersion = app.localVersion ?: app.db.getIgnoreVersion()
            when {
                VersionEntityUtils(app).isIgnored(
                    versionNumberList.first().versionInfo.name
                ) -> AppStatus.APP_LATEST
                localVersion == null -> AppStatus.APP_NO_LOCAL
                !isLatestVersionNumber(
                    localVersion,
                    versionNumberList.map { it.versionInfo }
                ) -> AppStatus.APP_OUTDATED
                else -> AppStatus.APP_LATEST
            }
        }
        return status
    }

    private fun isLatestVersionNumber(
        localVersion: VersionInfo, versionList: List<VersionInfo>
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
}