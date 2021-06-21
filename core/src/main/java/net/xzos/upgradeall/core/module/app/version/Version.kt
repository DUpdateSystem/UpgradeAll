package net.xzos.upgradeall.core.module.app.version

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.module.app.version_item.Asset
import net.xzos.upgradeall.core.utils.VersioningUtils
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList

/**
 * 版本号数据
 */
class Version(
    val rawVersionStringList: List<Pair<Char, Boolean>>,
    /* 资源列表 */
    val assetList: CoroutinesMutableList<Asset>,
    private val versionUtils: VersionUtils,
) : Comparable<Version> {

    /* 版本号 */
    val name: String = VersionUtils.getKey(rawVersionStringList)

    val isIgnored: Boolean get() = runBlocking { versionUtils.isIgnored(name) }

    suspend fun switchIgnoreStatus() {
        versionUtils.switchIgnoreStatus(name)
    }

    override fun compareTo(other: Version): Int {
        return VersioningUtils.compareVersionNumber(other.name, name) ?: -1
    }
}

val versionComparator = Comparator { v1: Version, v2: Version ->
    v1.compareTo(v2)
}