package net.xzos.upgradeall.core.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import net.xzos.upgradeall.core.data.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.data.ANDROID_CUSTOM_SHELL
import net.xzos.upgradeall.core.data.ANDROID_CUSTOM_SHELL_ROOT
import net.xzos.upgradeall.core.data.ANDROID_MAGISK_MODULE_TYPE
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.shell.getFileText
import java.io.StringReader
import java.security.MessageDigest
import java.util.*


fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    md.update(this.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun getProp(path: String): Properties? {
    return getFileText(path)?.parseProperties()
}

fun getAllLocalKeyList(): List<String> {
    return hashSetOf(
        ANDROID_APP_TYPE,
        ANDROID_MAGISK_MODULE_TYPE,
        ANDROID_CUSTOM_SHELL,
        ANDROID_CUSTOM_SHELL_ROOT
    ).apply {
        for (hub in HubManager.getHubList()) {
            for (urlTemp in hub.hubConfig.appUrlTemplates) {
                val keys =
                    AutoTemplate.getArgsKeywords(urlTemp).map { it.value.replaceFirst("%", "") }
                addAll(keys)
            }
        }
    }.toList()
}

fun getAppName(context: Context): String? {
    val packageName = context.packageName
    val pm = context.applicationContext.packageManager
    val ai = try {
        pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    return pm.getApplicationLabel(ai ?: return null).toString()
}

fun openInFileManager(path: String, context: Context) {
    val selectedUri = Uri.parse(path)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(selectedUri, "resource/folder")
    context.startActivity(intent)
}

fun <K> Map<K, String?>.cleanBlankValue(): Map<K, String> {
    return this.filterNot { it.value.isNullOrBlank() } as Map<K, String>
}

fun <K, V> MutableMap<K, V>.chunked(size: Int): List<MutableMap<K, V>> {
    val list = mutableListOf<MutableMap<K, V>>()
    var map = mutableMapOf<K, V>()
    this.forEach {
        map[it.key] = it.value
        if (map.size == size) {
            list.add(map)
            map = mutableMapOf()
        }
    }
    list.add(map)
    return list
}