package net.xzos.upgradeall.data.backup

import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.data.database.RepoDatabase
import net.xzos.upgradeall.utils.file.FileUtil
import net.xzos.upgradeall.utils.file.ZipFile
import org.json.JSONObject
import org.litepal.LitePal
import org.litepal.extension.findAll
import java.io.IOException

class BackupManager {
    private val appDatabaseIndex: JSONObject = JSONObject()
    fun mkZipFileBytes(): ByteArray? {
        return try {
            val zipFile = ZipFile()
            // backup database
            val allAppDatabase = backupAllAppDatabase()
            zipFile.zipByteFile(allAppDatabase.toString().toByteArray(), "app_database.json", "database")
            val allHubDatabase = backupAllHubDatabase()
            zipFile.zipByteFile(allHubDatabase.toString().toByteArray(), "hub_database.json", "database")
            val databaseIndex = appDatabaseIndex.toString()
            zipFile.zipByteFile(databaseIndex.toByteArray(), "app_database_index.json", "database")
            // backup ui
            zipFile.zipFile(FileUtil.UI_CONFIG_FILE, "ui")
            zipFile.zipFile(FileUtil.PREFERENCES_FILE)
            val imageDir = FileUtil.IMAGE_DIR
            zipFile.zipDirectory(imageDir, "ui/${imageDir.name}")

            zipFile.getByteArray()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            null
        }
    }

    fun backupAllAppDatabase(): JSONObject {
        val databaseList: List<RepoDatabase> = LitePal.findAll()
        val json = JSONObject()
        for (database in databaseList) {
            val data = database.toJson()
            val key = database.md5()
            json.put(key, data)
            // mk database index
            val id = database.id
            appDatabaseIndex.put(id.toString(), key)
        }
        return json
    }

    fun backupAllHubDatabase(): JSONObject {
        val databaseList: List<HubDatabase> = LitePal.findAll()
        val json = JSONObject()
        for (database in databaseList) {
            val data = database.toJson()
            val key = database.md5()
            json.put(key, data)
        }
        return json
    }
}
