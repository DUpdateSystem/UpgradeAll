package net.xzos.upgradeall.jscore.utils

import org.apache.maven.artifact.versioning.DefaultArtifactVersion


object VersioningUtils {

    private val versionRegex = "(\\d+(\\.\\d+)*)(([.|\\-|+|_| ]|[0-9A-Za-z])*)".toRegex()

    fun matchVersioningString(versionString: CharSequence?): String? {
        return if (versionString != null) {
            versionRegex.find(versionString)?.value
        } else null
    }

    /**
     * 对比 versionNumber0 与 versionNumber1
     * 若，前者比后者大，则返回 true*/
    fun compareVersionNumber(versioning0: String?, versioning1: String?): Boolean {
        val matchVersioning0 = matchVersioningString(versioning0)
        val matchVersioning1 = matchVersioningString(versioning1)
        return if (matchVersioning0 != null && matchVersioning1 != null) {
            val version0 = DefaultArtifactVersion(matchVersioning0)
            val version1 = DefaultArtifactVersion(matchVersioning1)
            version0 >= version1
        } else false
    }
}
