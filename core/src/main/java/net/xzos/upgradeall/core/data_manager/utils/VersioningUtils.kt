package net.xzos.upgradeall.core.data_manager.utils

import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import org.apache.maven.artifact.versioning.DefaultArtifactVersion


object VersioningUtils {

    private const val TAG = "VersioningUtils"
    private val objectTag = ObjectTag("Core", TAG)

    const val FOREVER_IGNORE = "FOREVER_IGNORE"

    fun matchVersioningString(versionString: CharSequence?): String? {
        return if (versionString != null) {
            val regex = AppValue.version_number_match_regex
            regex.find(versionString)?.value
        } else null
    }

    /**
     * 对比 versionNumber0 与 versionNumber1
     * 若，前者比后者大，则返回 true*/
    internal fun compareVersionNumber(versionNumber0: String?, versionNumber1: String?): Boolean {
        // 检查强制忽略版本号
        if (versionNumber0 == FOREVER_IGNORE) return true
        if (versionNumber1 == FOREVER_IGNORE) return false
        // 检查版本号是否相同
        if (versionNumber0 == versionNumber1) return true
        if (versionNumber0 != null && versionNumber1 == null) return true
        // 正常处理版本号
        val matchVersioning0 = matchVersioningString(versionNumber0)
        val matchVersioning1 = matchVersioningString(versionNumber1)
        /*
        接口趋于稳定，取消日志记录
        Log.i(
                objectTag,
                TAG, """original versioning:
                |0: $versionNumber0, 1: $versionNumber1
                |Fix: 0: $matchVersioning0, 1: $matchVersioning1""".trimMargin()
        )
         */
        return if (matchVersioning0 != null && matchVersioning1 != null) {
            val version0 = DefaultArtifactVersion(matchVersioning0)
            val version1 = DefaultArtifactVersion(matchVersioning1)
            version0 >= version1
        } else false
    }
}
