package net.xzos.upgradeall.data.backup

import com.google.gson.Gson
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.data_manager.HubDatabaseManager
import net.xzos.upgradeall.data.gson.UIConfig
import net.xzos.upgradeall.data.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.data.gson.changeAppDatabaseId
import net.xzos.upgradeall.data.gson.parseUiConfig
import net.xzos.upgradeall.data.gson.toUiConfigId
import net.xzos.upgradeall.utils.file.FileUtil
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
            "database/applications_database.json" -> restoreApplicationsDatabase(JSONArray(text))
            "database/hub_database.json" -> restoreHubDatabase(JSONArray(text))
            "/${context.packageName}_preferences.xml" -> {
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
        val databaseIdMap = getDatabaseIdMap()
        uiConfigBackup.changeAppDatabaseId(databaseIdMap)
        restoreUiConfig(uiConfigBackup)
    }

    private fun restoreUiConfig(uiConfigBackup: UIConfig) {
        uiConfig.updateTab = uiConfigBackup.updateTab
        uiConfig.allAppTab = uiConfigBackup.allAppTab
        with(uiConfig.userStarTab) {
            name = uiConfigBackup.userStarTab.name
            icon = uiConfigBackup.userStarTab.icon
            enable = uiConfigBackup.userStarTab.enable
            addItemList(uiConfigBackup.userStarTab.itemList)
        }
        val searchUserTab = fun(name: String): UIConfig.CustomContainerTabListBean? {
            for (userTab in uiConfig.userTabList) {
                if (name == userTab.name)
                    return userTab
            }
            return null
        }
        var i = 0
        for (user_tab in uiConfigBackup.userTabList) {
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

    private fun getDatabaseIdMap(): Map<String, String> {
        val databaseIdMap = mutableMapOf<String, String>()
        for (database in AppDatabaseManager.appDatabases) {
            databaseIdMap[database.toUiConfigId()] = database.md5()
        }
        for (database in AppDatabaseManager.applicationsDatabases) {
            databaseIdMap[database.toUiConfigId()] = database.md5()
        }
        return databaseIdMap
    }

    private suspend fun restoreAppDatabase(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            val database = parseAppDatabaseConfig(jsonArray.getJSONObject(i))
            AppDatabaseManager.insertAppDatabase(database)
        }
    }

    private suspend fun restoreApplicationsDatabase(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            val database = parseApplicationsDatabaseConfig(jsonArray.getJSONObject(i))
            AppDatabaseManager.insertApplicationsDatabase(database)
        }
    }

    private suspend fun restoreHubDatabase(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            val s = jsonArray.getString(i)
            val gson = Gson().fromJson(s, HubConfigGson::class.java)
            HubDatabaseManager.addDatabase(gson)
        }
    }
}
