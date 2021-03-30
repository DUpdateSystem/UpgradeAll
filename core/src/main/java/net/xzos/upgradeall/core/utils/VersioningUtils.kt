package net.xzos.upgradeall.core.utils

import net.xzos.upgradeall.core.data.VERSION_NUMBER_MATCH_REGEX
import net.xzos.upgradeall.core.data.VERSION_NUMBER_STRICT_MATCH_REGEX
import net.xzos.upgradeall.core.log.ObjectTag
import net.xzos.upgradeall.core.module.app.Version
import org.apache.maven.artifact.versioning.DefaultArtifactVersion


object VersioningUtils {

    private const val TAG = "VersioningUtils"
    private val objectTag = ObjectTag("Core", TAG)

    const val FOREVER_IGNORE = "FOREVER_IGNORE"

    fun matchVersioningString(versionString: CharSequence?): String? {
        return if (versionString != null) {
            (VERSION_NUMBER_STRICT_MATCH_REGEX.find(versionString)
                    ?: VERSION_NUMBER_MATCH_REGEX.find(versionString))?.value
        } else null
    }

    /**
     * 对比 versionNumber0 与 versionNumber1
     * 若，前者比后者大，则返回 1，反之，返回 -1，相等返回 0
     * 若，版本号不是语义化版本号，返回 NULL
     */
    fun compareVersionNumber(versionNumber0: String?, versionNumber1: String?): Int? {
        // 检查强制忽略版本号
        if (versionNumber0 == FOREVER_IGNORE) return 1
        if (versionNumber1 == FOREVER_IGNORE) return -1
        // 检查版本号是否相同
        if (versionNumber0 == versionNumber1) return 1
        if (versionNumber0 != null && versionNumber1 == null) return 1
        // 正常处理版本号
        val matchVersioning0 = matchVersioningString(versionNumber0)
        val matchVersioning1 = matchVersioningString(versionNumber1)
        if (matchVersioning0.isNullOrBlank() || matchVersioning1.isNullOrBlank()) {
            return null
        }
        /*
        接口趋于稳定，取消日志记录
        Log.i(
                objectTag,
                TAG, """original versioning:
                |0: $versionNumber0, 1: $versionNumber1
                |Fix: 0: $matchVersioning0, 1: $matchVersioning1""".trimMargin()
        )
         */
        val version0 = DefaultArtifactVersion(matchVersioning0)
        val version1 = DefaultArtifactVersion(matchVersioning1)
        return when {
            version0 > version1 -> 1
            version0 == version1 -> 0
            else -> -1
        }
    }
}