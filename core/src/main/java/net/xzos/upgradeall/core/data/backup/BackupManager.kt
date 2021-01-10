package net.xzos.upgradeall.core.data.backup

import android.annotation.SuppressLint
import android.os.Build
import com.google.gson.Gson
import net.xzos.upgradeall.core.data.json.UIConfig
import net.xzos.upgradeall.core.data.json.uiConfig
import net.xzos.upgradeall.core.database.metaDatabase
import net.xzos.upgradeall.core.utils.file.FileUtil
import net.xzos.upgradeall.core.utils.file.ZipFile
import org.json.JSONArray
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
            @SuppressLint("SimpleDateFormat")
            val formatter = SimpleDateFormat(dataFormat)
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

    private suspend fun backupAllAppDatabase(): JSONArray {
        val databaseList = metaDatabase.appDao().loadAll()
        val json = JSONArray()
        for (database in databaseList) {
            val data = database.toJson()
            json.put(data)
        }
        return json
    }

    private suspend fun backupAllHubDatabase(): JSONArray {
        val databaseList = metaDatabase.hubDao().loadAll()
        val json = JSONArray()
        for (database in databaseList) {
            val data = database.toJson()
            json.put(data)
        }
        return json
    }

    private fun backupUiConfig(): UIConfig {
        return uiConfig.copy()
    }
}
