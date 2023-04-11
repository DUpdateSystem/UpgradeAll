package net.xzos.upgradeall.core.module.app.data

import net.xzos.upgradeall.core.module.Hub
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionInfo
import net.xzos.upgradeall.core.module.app.version.VersionWrapper
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableMapOf
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class VersionMap private constructor() {
    private var ignoreVersionNumberRegex: String? = null
    private var includeVersionNumberRegex: String? = null

    internal var hubStatus = coroutinesMutableMapOf<Hub, HubStatus>(true)
    fun isRenewing() = hubStatus.containsValue(HubStatus.RENEW)

    /* 版本号数据列表 */
    private val versionMap: CoroutinesMutableMap<VersionInfo, MutableSet<VersionWrapper>> =
        coroutinesMutableMapOf()

    private var versionList: List<Version>? = null

    fun setVersionNumberRegex(ignore: String?, include: String?) {
        ignoreVersionNumberRegex = ignore
        includeVersionNumberRegex = include
        refreshMap()
    }

    private fun refreshMap() {
        versionMap.toMap().also {
            versionMap.clear()
        }.forEach {
            addReleaseList(it.value)
        }
    }

    fun addReleaseList(releaseList: Collection<VersionWrapper>) {
        releaseList.forEach {
            val versionInfo = getVersionInfo(it.release)
            versionMap.getOrPut(versionInfo) { mutableSetOf() }.add(it)
        }
        releaseList.firstOrNull()?.let {
            hubStatus[it.hub] = HubStatus.FULL
        }
        versionList = null
    }

    fun addSingleRelease(release: VersionWrapper) {
        val versionInfo = getVersionInfo(release.release)
        versionMap.getOrPut(versionInfo) { mutableSetOf() }.add(release)
        hubStatus[release.hub] = HubStatus.SINGLE
        versionList = null
    }

    fun setError(hub: Hub) {
        hubStatus[hub] = HubStatus.ERROR
    }

    private fun getVersionInfo(release: ReleaseGson): VersionInfo {
        return VersionInfo.new(
            release.versionNumber,
            ignoreVersionNumberRegex,
            includeVersionNumberRegex,
            release.extra ?: emptyMap()
        )
    }

    private fun sortList(): List<Version> {
        return versionList ?: versionMap.keys.filter { it.name.isNotBlank() }.sortedDescending()
            .map { Version(it, versionMap[it]?.toList() ?: emptyList()) }
            .apply { versionList = this }
    }

    fun getVersionList(): List<Version> = sortList()

    companion object {
        fun new(
            ignoreVersionNumberRegex: String?,
            includeVersionNumberRegex: String?,
        ) = VersionMap().apply {
            setVersionNumberRegex(ignoreVersionNumberRegex, includeVersionNumberRegex)
        }

        internal enum class HubStatus {
            RENEW,
            ERROR,
            SINGLE,
            FULL,
        }
    }
}