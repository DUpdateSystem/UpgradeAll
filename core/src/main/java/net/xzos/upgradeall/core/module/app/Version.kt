package net.xzos.upgradeall.core.module.app

import net.xzos.upgradeall.core.utils.VersioningUtils
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList

/**
 * 版本号数据
 */
class Version(
        /* 版本号 */
        val name: String,
        /* 资源列表 */
        val assetList: CoroutinesMutableList<Asset>,
        private val versionUtils: VersionUtils,
) : Comparable<Version> {

    fun getShowName(): Triple<String, String, String> {
        val prefixList = mutableListOf<String>()
        val suffixList = mutableListOf<String>()
        assetList.forEach {
            val list = it.versionNumber.split(name, limit = 2)
            list.firstOrNull()?.run {
                if (isNotBlank()) prefixList.add(this)
            }
            list.lastOrNull()?.run {
                if (this.isNotBlank()) suffixList.add(this)
            }
        }
        val prefixString = if (prefixList.isNotEmpty())
            prefixList.joinToString(prefix = "(", separator = "/", postfix = ")")
        else ""
        val suffixString = if (suffixList.isNotEmpty())
            suffixList.joinToString(prefix = "(", separator = "/", postfix = ")")
        else ""
        return Triple(prefixString, name, suffixString)
    }

    val isIgnored: Boolean get() = versionUtils.isIgnored(name)

    fun switchIgnoreStatus() {
        versionUtils.switchIgnoreStatus(name)
    }

    override fun compareTo(other: Version): Int {
        return VersioningUtils.compareVersionNumber(other.name, name) ?: -1
    }
}

val versionComparator = Comparator { v1: Version, v2: Version ->
    v1.compareTo(v2)
}