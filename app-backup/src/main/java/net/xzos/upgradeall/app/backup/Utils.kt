package net.xzos.upgradeall.app.backup

import net.xzos.upgradeall.core.androidutils.androidContext
import java.io.File


val preferencesFile = File(
    androidContext.filesDir.parentFile,
    "shared_prefs/${androidContext.packageName}_preferences.xml"
)
