package net.xzos.upgradeall.app.backup

import android.annotation.SuppressLint
import android.content.Context
import java.io.File

@SuppressLint("StaticFieldLeak")
internal lateinit var androidContext: Context

val preferencesFile = File(
    androidContext.filesDir.parentFile,
    "shared_prefs/${androidContext.packageName}_preferences.xml"
)
