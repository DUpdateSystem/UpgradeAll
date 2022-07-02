package net.xzos.upgradeall.core.module.app.version

import net.xzos.upgradeall.core.utils.constant.VERSION_CODE
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils

@Suppress("DataClassPrivateConstructor")
data class VersionInfo private constructor(
    val name: String,
    val extra: Map<String, Any?>,
) : Comparable<VersionInfo> {
    lateinit var versionCharList: List<Pair<Char, Boolean>>

    override fun compareTo(other: VersionInfo): Int {
        compareExtra(other, VERSION_CODE) { 0 }.let {
            if (it != 0) return it
        }
        return VersioningUtils.compareVersionNumber(other.name, name) ?: -1
    }

    private fun <V : Comparable<V>> compareExtra(
        other: VersionInfo, key: String, defaultValue: () -> V
    ): Int {
        return extra.getOrElseWrap(key, defaultValue).compareTo(
            other.extra.getOrElseWrap(key, defaultValue)
        )
    }

    companion object {
        fun new(
            versionName: String,
            ignoreVersionNumberRegex: String? = null,
            extra: Map<String, Any?> = mapOf(),
        ): VersionInfo {
            val versionCharList = getVersionNumberCharString(versionName, ignoreVersionNumberRegex)
            return VersionInfo(
                versionCharList.filter { it.second }.joinToString(""), extra
            ).apply {
                this.versionCharList = versionCharList
            }
        }
    }
}

private fun <V> Map<String, Any?>.getOrElseWrap(key: String, defaultValue: () -> V): V {
    @Suppress("UNCHECKED_CAST")
    return this.getOrElse(key, defaultValue) as V
}