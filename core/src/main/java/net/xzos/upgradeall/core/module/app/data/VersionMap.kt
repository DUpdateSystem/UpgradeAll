package net.xzos.upgradeall.core.module.app.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.xzos.upgradeall.core.module.app.version.Version
import net.xzos.upgradeall.core.module.app.version.VersionInfo
import net.xzos.upgradeall.core.module.app.version.VersionWrapper
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

class VersionMap private constructor() {
    private var ignoreVersionNumberRegex: String? = null

    private val mutex = Mutex()

    /* 版本号数据列表 */
    private var versionMap: MutableMap<VersionInfo, MutableSet<VersionWrapper>> = mutableMapOf()

    private var versionList: List<Version> = listOf()
    private var sorted
        get() = !(versionMap.isNotEmpty() && versionList.isEmpty())
        set(value) {
            if (!value) versionList = listOf()
        }

    suspend fun setIgnoreVersionNumberRegex(regex: String) {
        ignoreVersionNumberRegex = regex
        refreshMap()
    }

    private suspend fun refreshMap() {
        versionMap.toMap().also {
            versionMap = mutableMapOf()
        }.forEach {
            it.value.forEach { release ->
                addRelease(release)
            }
        }
    }

    suspend fun addRelease(release: VersionWrapper) {
        sorted = false
        mutex.withLock {
            val versionInfo = getVersionInfo(release.release)
            versionMap.getOrPut(versionInfo) { mutableSetOf() }.add(release)
        }
    }

    private fun getVersionInfo(release: ReleaseGson): VersionInfo {
        return VersionInfo.new(
            release.versionNumber, ignoreVersionNumberRegex, release.extra ?: emptyMap()
        )
    }

    private suspend fun sortList(): List<Version> {
        if (!sorted)
            mutex.withLock {
                if (sorted) return@withLock  // 优化等待锁后已经排序的情况
                versionList = versionMap.keys.filter { it.name.isNotBlank() }.sortedDescending()
                    .map { Version(it, versionMap[it]?.toList() ?: emptyList()) }
            }
        return versionList
    }

    fun getVersionList(): List<Version> = runBlocking { sortList() }

    companion object {
        suspend fun new(
            ignoreVersionNumberRegex: String? = null
        ) = VersionMap().apply {
            ignoreVersionNumberRegex?.let {
                setIgnoreVersionNumberRegex(it)
            }
        }
    }
}