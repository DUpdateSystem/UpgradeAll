package net.xzos.upgradeall.core.data_manager.utils

import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import org.apache.maven.artifact.versioning.DefaultArtifactVersion


object VersioningUtils {

    private const val TAG = "VersioningUtils"
    private val objectTag = ObjectTag("Core", TAG)

    fun matchVersioningString(versionString: CharSequence?): String? {
        return if (versionString != null) {
            val regex = AppConfig.version_number_match_regex
            regex.find(versionString)?.value
        } else null
    }

    /**
     * 对比 versionNumber0 与 versionNumber1
     * 若，前者比后者大，则返回 true*/
    internal fun compareVersionNumber(versioning0: String?, versioning1: String?): Boolean {
        val matchVersioning0 = matchVersioningString(versioning0)
        val matchVersioning1 = matchVersioningString(versioning1)
        Log.i(
                objectTag,
                TAG, """original versioning: 
                |0: $versioning0, 1: $versioning1
                |Fix: 0: $matchVersioning0, 1: $matchVersioning1""".trimMargin()
        )
        return if (matchVersioning0 != null && matchVersioning1 != null) {
            val version0 = DefaultArtifactVersion(matchVersioning0)
            val version1 = DefaultArtifactVersion(matchVersioning1)
            version0 >= version1
        } else false
    }
}