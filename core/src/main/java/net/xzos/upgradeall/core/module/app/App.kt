package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.androidutils.app_info.getAppVersion
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.data.AppDbWrapper
import net.xzos.upgradeall.core.module.app.data.DataGetter
import net.xzos.upgradeall.core.module.app.data.VersionMap
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionInfo
import net.xzos.upgradeall.core.websdk.json.AppConfigGson

data class App(override val db: AppEntity) : AppDbWrapper() {

    // Version 信息
    val versionMap: VersionMap =
        VersionMap.new(
            db.invalidVersionNumberFieldRegexString,
            db.includeVersionNumberFieldRegexString
        )

    /* 这个 App 数据可用的软件源 */
    val hubAvailableList: List<Hub>
        get() = versionList.flatMap { version -> version.versionList.map { it.hub } }

    /* App 在本地的版本号 */
    val localVersion: VersionInfo?
        get() {
            val appVersionInfo = getAppVersion(db.appId) ?: return null
            return VersionInfo.new(
                appVersionInfo.name,
                db.invalidVersionNumberFieldRegexString,
                db.includeVersionNumberFieldRegexString,
                appVersionInfo.extra,
            )
        }

    /* 版本号数据列表 */
    val versionList: List<Version>
        get() {
            if (versionMap.status != VersionMap.Companion.VersionStatus.COMPLETE)
                DataGetter.getVersionList(this)
            return versionMap.getVersionList()
        }

    val cloudConfig: AppConfigGson? get() = db.cloudConfig

    /* 刷新版本号数据 */
    fun update() {
        DataGetter.getLatestVersion(this)
    }

    fun isRenewing(): Boolean {
        return versionMap.status != VersionMap.Companion.VersionStatus.SIMPLE
                || versionMap.status != VersionMap.Companion.VersionStatus.COMPLETE
    }

    /* 获取 App 的更新状态 */
    fun getReleaseStatus(): AppStatus {
        return Updater.getReleaseStatus(this)
    }

    fun getLatestVersion(): VersionInfo? {
        return if (isLatestVersion())
            localVersion ?: db.getIgnoreVersion()
        else versionList.firstOrNull()?.versionInfo
    }

    /* 获取 App 的更新状态 */
    fun isLatestVersion(): Boolean {
        return getReleaseStatus() == AppStatus.APP_LATEST
    }
}