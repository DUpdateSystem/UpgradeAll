package net.xzos.upgradeall.core.module.app.version

import net.xzos.upgradeall.core.utils.constant.VERSION_CODE
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils

@Suppress("DataClassPrivateConstructor")
data class VersionInfo private constructor(
    val name: String,
    val extra: Map<String, Any?>,
) : Comparable<VersionInfo> {
    var versionCharList: List<Pair<Char, Boolean>> = listOf()
        set(value) {
            field = value + printExtraChar()
        }

    private fun printExtraChar() = if (extra.isNotEmpty())
        extra.values.joinToString(",", "(", ")") {
            if (it is Number) {
                it.toString().substringBefore(".")
            } else it.toString()
        }.map { Pair(it, true) }
    else emptyList()

    fun compareToOrError(other: VersionInfo): Int? {
        compareExtra(other, VERSION_CODE) {
            if (it is Long) it
            else it.toString().toDouble().toLong()
        }?.let {
            if (it != 0) return it
        }
        return VersioningUtils.compareVersionNumber(other.name, name)
    }

    override fun compareTo(other: VersionInfo): Int {
        return compareToOrError(other) ?: -1
    }

    private fun <V : Comparable<V>> compareExtra(
        other: VersionInfo, key: String, transfer: (Any) -> V
    ): Int? {
        return transfer(extra[key] ?: return null).compareTo(
            transfer(other.extra[key] ?: return null)
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
                versionCharList.mapNotNull { if (it.second) it.first else null }.joinToString(""),
                extra
            ).apply {
                this.versionCharList = versionCharList
            }
        }
    }
}