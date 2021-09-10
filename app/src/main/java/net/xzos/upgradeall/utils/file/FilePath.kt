package net.xzos.upgradeall.utils.file

import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.androidutils.getExistsFile
import java.io.File

val PREFERENCES_FILE by lazy {
    File(
        context.filesDir.parentFile,
        "shared_prefs/${context.packageName}_preferences.xml"
    )
}
private val CACHE_DIR = context.externalCacheDir!!
val DOWNLOAD_CACHE_DIR by lazy { File(CACHE_DIR, "Download").getExistsFile(true) }
val DOWNLOAD_EXTRA_CACHE_DIR by lazy {
    File(DOWNLOAD_CACHE_DIR, "ExtraCache").getExistsFile(true).apply {
        deleteChildRecursive()
    }
}

fun refreshStorage() {
}