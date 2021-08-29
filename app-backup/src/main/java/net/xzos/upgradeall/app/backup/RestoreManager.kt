package net.xzos.upgradeall.app.backup

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.manager.HubManager
import net.xzos.upgradeall.core.utils.file.parseZipBytes
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


object RestoreManager {
    private const val TAG = "RestoreManager"
    private val logObjectTag = ObjectTag("app-backup", TAG)

    suspend fun parseZip(zipFileByteArray: ByteArray) {
        parseZipBytes(zipFileByteArray) { name, bytes ->
            runBlocking { parseData(name, bytes) }
            false
        }
    }

    private suspend fun parseData(name: String, bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
        when (name) {
            "database/app_database.json" -> restoreAppDatabase(JSONArray(text))
            "database/hub_database.json" -> restoreHubDatabase(JSONArray(text))
            "/${preferencesFile.name}" -> preferencesFile.writeText(text)
            else -> {
                Log.e(logObjectTag, TAG, "ignore file: $name")
            }
        }
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
