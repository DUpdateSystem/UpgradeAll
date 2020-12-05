package net.xzos.upgradeall.data.backup

import android.os.Build
import com.google.gson.Gson
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.data.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.data.gson.changeAppDatabaseId
import net.xzos.upgradeall.data.gson.toUiConfigId
import net.xzos.upgradeall.utils.file.FileUtil
import net.xzos.upgradeall.utils.file.ZipFile
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

    fun mkZipFileBytes(): ByteArray? {
        return try {
            val zipFile = ZipFile()
            // backup database
            val allAppDatabase = backupAllAppDatabase()
            zipFile.zipByteFile(allAppDatabase.toString().toByteArray(), "app_database.json", "database")
            val allApplicationsDatabase = backupAllApplicationsDatabase()
            zipFile.zipByteFile(allApplicationsDatabase.toString().toByteArray(), "applications_database.json", "database")
            val allHubDatabase = backupAllHubDatabase()
            zipFile.zipByteFile(allHubDatabase.toString().toByteArray(), "hub_database.json", "database")
            // backup ui
            val uiConfig = backupUiConfig()
            zipFile.zipByteFile(Gson().toJson(uiConfig).toByteArray(), FileUtil.UI_CONFIG_FILE.name, "ui")
            zipFile.zipFile(FileUtil.PREFERENCES_FILE)
            val imageDir = FileUtil.IMAGE_DIR
            zipFile.zipDirectory(imageDir, "ui/${imageDir.name}")

            zipFile.getByteArray()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        }
    }

    private fun backupAllAppDatabase(): JSONArray {
        val databaseList = AppDatabaseManager.appDatabases
        val json = JSONArray()
        var data: JSONObject
        for (database in databaseList) {
            data = database.toJson()
            json.put(data)
        }
        return json
    }

    private fun backupAllApplicationsDatabase(): JSONArray {
        val databaseList = AppDatabaseManager.applicationsDatabases
        val json = JSONArray()
        var data: JSONObject
        for (database in databaseList) {
            data = database.toJson()
            json.put(data)
        }
        return json
    }

    private fun backupAllHubDatabase(): JSONArray {
        val databaseList = HubDatabaseManager.hubDatabases
        val json = JSONArray()
        var data: JSONObject
        for (database in databaseList) {
            data = database.toJson()
            json.put(data)
        }
        return json
    }

    private fun backupUiConfig(): UIConfig {
        val uiConfig = uiConfig.copy()
        val databaseIdMap = mutableMapOf<String, String>()
        for (database in AppDatabaseManager.appDatabases) {
            databaseIdMap[database.toUiConfigId()] = database.md5()
        }
        for (database in AppDatabaseManager.applicationsDatabases) {
            databaseIdMap[database.toUiConfigId()] = database.md5()
        }
        uiConfig.changeAppDatabaseId(databaseIdMap)
        return uiConfig
    }
}
