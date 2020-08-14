package net.xzos.upgradeall.data.backup

import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.data.database.HubDatabase
import net.xzos.upgradeall.data.database.RepoDatabase
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.data.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.utils.file.FileUtil
import org.json.JSONObject
import org.litepal.LitePal
import org.litepal.extension.findAll
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class RestoreManager {
    private val backupManager = BackupManager()
    private var appDatabaseIndex: JSONObject? = null
    private var uiConfigBackup: UIConfig? = null

    fun parseZip(zipFileByteArray: ByteArray) {
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

    private fun parseData(name: String, bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
        when (name) {
            "database/app_database.json" -> restoreAppDatabase(JSONObject(text))
            "database/hub_database.json" -> restoreHubDatabase(JSONObject(text))
            "database/app_database_index.json" -> parseAppDatabaseIndex(text)
            "/${context.packageName}_preferences.xml" -> {
                FileUtil.PREFERENCES_FILE.writeText(text)
            }
            "ui/ui.json" -> parseUiConfig(text)
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

    private fun parseAppDatabaseIndex(row_str: String) {
        appDatabaseIndex = JSONObject(row_str)
        restoreUiConfig()
    }

    private fun parseUiConfig(row_str: String) {
        uiConfigBackup = UIConfig.parseUiConfig(row_str)
        restoreUiConfig()
    }

    private fun restoreUiConfig() {
        if (appDatabaseIndex == null || uiConfigBackup == null) return
        uiConfig.updateTab = uiConfigBackup!!.updateTab
        uiConfig.allAppTab = uiConfigBackup!!.allAppTab
        with(uiConfig.userStarTab) {
            name = uiConfigBackup!!.userStarTab.name
            icon = uiConfigBackup!!.userStarTab.icon
            enable = uiConfigBackup!!.userStarTab.enable
            addItemList(uiConfigBackup!!.userStarTab.itemList)
        }
        val searchUserTab = fun(name: String): UIConfig.CustomContainerTabListBean? {
            for (userTab in uiConfig.userTabList) {
                if (name == userTab.name)
                    return userTab
            }
            return null
        }
        var i = 0
        for (user_tab in uiConfigBackup!!.userTabList) {
            i += 1
            val name = user_tab.name
            val userTab = searchUserTab(name) ?: UIConfig.CustomContainerTabListBean(name).apply {
                uiConfig.userTabList.add(i, this)
            }
            userTab.icon = user_tab.icon
            userTab.addItemList(user_tab.itemList)
        }
        uiConfig.save()
    }

    private fun getNewestAppDatabaseId(oldId: Long, keyMap: Map<String, Long>): Long? {
        appDatabaseIndex?.getString(oldId.toString())?.let { key ->
            return keyMap[key]
        }
        return null
    }

    private fun getDatabaseIdMap(): Map<String, Long> {
        val keyMap = mutableMapOf<String, Long>()
        val databaseList: List<RepoDatabase> = LitePal.findAll()
        for (database in databaseList) {
            val key = database.md5()
            val id = database.id
            keyMap[key] = id
        }
        return keyMap
    }

    private fun restoreAppDatabase(json: JSONObject) {
        val existAppDatabaseKey = backupManager.backupAllAppDatabase().keys().asSequence().toList()
        val existAppDatabaseKey1 = backupManager.backupAllAppDatabase().keys().asSequence().toList()
        val keys = json.keys().asSequence().toList().filter {
            !existAppDatabaseKey.contains(it)
        }
        for (k in keys) {
            RepoDatabase("", "", "", "").apply {
                parseFromJson(json.getJSONObject(k))
            }.save()
        }
    }

    private fun restoreHubDatabase(json: JSONObject) {
        val existAppDatabaseKey = backupManager.backupAllHubDatabase().keys().asSequence().toList()
        for (k in json.keys().asSequence().toList().filter {
            !existAppDatabaseKey.contains(it)
        }) {
            HubDatabase("", "").apply {
                parseFromJson(json.getJSONObject(k))
            }.save()
        }
    }
}
