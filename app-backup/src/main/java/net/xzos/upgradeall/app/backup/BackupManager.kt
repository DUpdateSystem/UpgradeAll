package net.xzos.upgradeall.app.backup

import android.os.Build
import net.xzos.upgradeall.core.data.json.UIConfig
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.database.metaDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object BackupManager {

    fun newFileName(): String {
        val dataFormat = "yyyy-MM-dd_HH-mm"
        val timeString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern(dataFormat)
            current.format(formatter)
        } else {
            val formatter = SimpleDateFormat(dataFormat, Locale.getDefault())
            formatter.format(Date())
        }
        return "UpgradeAll_$timeString.zip"
    }

    suspend fun mkZipFileBytes(): ByteArray? {
        return try {
            val zipFile = ZipFile()
            // backup database
            val allAppDatabase = backupAllAppDatabase()
            zipFile.zipByteFile(allAppDatabase.toString().toByteArray(), "app_database.json", "database")
            val allHubDatabase = backupAllHubDatabase()
            zipFile.zipByteFile(allHubDatabase.toString().toByteArray(), "hub_database.json", "database")
            // backup ui
            zipFile.zipFile(preferencesFile)

            zipFile.getByteArray()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        }
    }

    private suspend fun backupAllAppDatabase(): JSONArray {
        val databaseList = metaDatabase.appDao().loadAll()
        val json = JSONArray()
        var data: JSONObject
        for (database in databaseList) {
            data = database.toJson()
            json.put(data)
        }
        return json
    }

    private suspend fun backupAllHubDatabase(): JSONArray {
        val databaseList = metaDatabase.hubDao().loadAll()
        val json = JSONArray()
        var data: JSONObject
        for (database in databaseList) {
            data = database.toJson()
            json.put(data)
        }
        return json
    }

    private fun backupUiConfig(): UIConfig {
        return uiConfig.copy()
    }
}
