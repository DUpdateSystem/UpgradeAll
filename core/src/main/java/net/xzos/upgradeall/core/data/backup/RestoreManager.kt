package net.xzos.upgradeall.core.data.backup

import net.xzos.upgradeall.core.coreConfig
import net.xzos.upgradeall.core.data.json.UIConfig
import net.xzos.upgradeall.core.data.json.parseUiConfig
import net.xzos.upgradeall.core.data.json.save
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.file.FileUtil
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


object RestoreManager {
    suspend fun parseZip(zipFileByteArray: ByteArray) {
        ZipInputStream(zipFileByteArray.inputStream()).use { zis ->
            var ze: ZipEntry?
            var count: Int
            val buffer = ByteArray(8192)
            while (zis.nextEntry.also { ze = it } != null) {
                val name = ze!!.name
                val byteArrayOutputStream = ByteArrayOutputStream()
                byteArrayOutputStream.use { it ->
                    while (zis.read(buffer).also { count = it } != -1) it.write(buffer, 0, count)
                }
                val bytes = byteArrayOutputStream.toByteArray()
                parseData(name, bytes)
            }
        }
    }

    private suspend fun parseData(name: String, bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
        when (name) {
            "database/app_database.json" -> restoreAppDatabase(JSONArray(text))
            "database/hub_database.json" -> restoreHubDatabase(JSONArray(text))
            "/${coreConfig.androidContext.packageName}_preferences.xml" -> {
                FileUtil.PREFERENCES_FILE.writeText(text)
            }
            "ui/ui.json" -> parseUiConfigString(text)
            else -> {
                when (name.substring(0, name.lastIndexOf("/"))) {
                    "ui/images" -> {
                        val fileName = name.substring(name.lastIndexOf("/"))
                        File(FileUtil.IMAGE_DIR, fileName).writeBytes(bytes)
                    }
                }
            }
        }
    }

    private fun parseUiConfigString(row_str: String) {
        val uiConfigBackup = parseUiConfig(row_str)
        restoreUiConfig(uiConfigBackup)
    }

    private fun restoreUiConfig(uiConfigBackup: UIConfig) {
        for (item in uiConfigBackup.user_star_app_id_list) {
            uiConfig.user_star_app_id_list.add(item)
        }
        uiConfig.save()
    }

    private suspend fun restoreAppDatabase(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            val s = jsonArray.getJSONObject(i)
            val entity = parseAppEntityConfig(s)
            AppManager.updateApp(entity)
        }
    }

    private suspend fun restoreHubDatabase(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            val s = jsonArray.getJSONObject(i)
            val entity = parseHubEntityConfig(s)
            HubManager.updateHub(entity)
        }
    }
}
