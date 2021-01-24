package net.xzos.upgradeall.core.database.migration

import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import net.xzos.upgradeall.core.data.backup.getOrNull
import net.xzos.upgradeall.core.utils.AutoTemplate
import net.xzos.upgradeall.core.utils.file.FileUtil
import org.json.JSONArray
import org.json.JSONObject


val MIGRATION_8_9 = object : Migration(8, 9) {
    val appDatabaseMap = mutableMapOf<Long, JSONObject>()
    val applicationDatabaseMap = mutableMapOf<Long, JSONObject>()
    val allHubDatabaseMap = mutableMapOf<String, JSONObject>()
    val hubDatabaseMap = mutableMapOf<String, JSONObject>()

    val ignoreVersionNumberMap = mutableMapOf<JSONObject, String>()

    override fun migrate(database: SupportSQLiteDatabase) {
        getOldDatabase(database)
        checkHubDatabase()
        renewAppId()
        renewUiConfig()
        deleteOldDatabase(database)
        renewDatabaseData()
        createNewDatabase(database)
    }

    private fun deleteOldDatabase(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX app_key_value")
        database.execSQL("DROP INDEX applications_key_value")
        database.execSQL("CREATE UNIQUE INDEX app_key_value on app (app_id)")
        database.execSQL("DROP TABLE app")
        database.execSQL("DROP TABLE applications")
        database.execSQL("DROP TABLE hub")
        database.execSQL("""
            CREATE TABLE app
            (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            app_id TEXT NOT NULL,
            ignore_version_number TEXT DEFAULT null,
            cloud_config TEXT DEFAULT null
            );
        """
        )
        database.execSQL("""
            CREATE TABLE hub
            (
            uuid INTEGER PRIMARY KEY NOT NULL,
            hub_config TEXT NOT NULL,
            auth TEXT DEFAULT NULL,
            ignore_app_id_list TEXT DEFAULT null,
            auto_ignore_app_id_list TEXT DEFAULT null
            );
        """
        )
    }

    private fun renewDatabaseData() {
        for (applicationJson in applicationDatabaseMap.values) {
            renewInvalidPackageList(applicationJson)
            renewIgnoreAppIdList(applicationJson)
        }
        renewIgnoreVersionNumber()
    }

    private fun createNewDatabase(database: SupportSQLiteDatabase) {
        for (appJson in appDatabaseMap.values) {
            database.execSQL("""
            INSERT INTO app (name, app_id, ignore_version_number, cloud_config)
            VALUES (${appJson.getString("name")}, ${appJson.getString("app_id")}, ${appJson.getOrNull("ignore_version_number")}, ${appJson.getOrNull("cloud_config")});
            """
            )
        }
        for (hubJson in hubDatabaseMap.values) {
            database.execSQL("""
            INSERT INTO hub(uuid, hub_config, ignore_app_id_list, auto_ignore_app_id_list)
            VALUES (${hubJson.getString("hub_uuid")}, ${hubJson.getString("hub_config")}, ${hubJson.getOrNull("ignore_app_id_list")}, ${hubJson.getOrNull("auto_ignore_app_id_list")});
            """
            )
        }
    }


    private fun renewIgnoreVersionNumber() {
        for (applicationJson in applicationDatabaseMap.values) {
            renewIgnoreVersionNumber(applicationJson)
        }
        for (appJson in appDatabaseMap.values) {
            if (appJson.getOrNull("ignore_version_number") != null) {
                val appId = appJson.getJSONObject("app_id")
                for (e in ignoreVersionNumberMap.entries) {
                    if (appId.similar(e.key)) {
                        appJson.put("ignore_version_number", e.value)
                        continue
                    }
                }
            }
        }
    }

    private fun JSONObject.similar(other: Any): Boolean {
        return try {
            if (other !is JSONObject) {
                return false
            }
            val set = this.keys().iterator().asSequence().toList()
            if (set != other.keys().iterator().asSequence().toList()) {
                return false
            }
            val iterator = set.iterator()
            while (iterator.hasNext()) {
                val name = iterator.next()
                val valueThis = this[name]
                val valueOther = other[name]
                if (valueThis is JSONObject) {
                    if (!valueThis.similar(valueOther)) {
                        return false
                    }
                } else if (valueThis != valueOther) {
                    return false
                }
            }
            true
        } catch (exception: Throwable) {
            false
        }
    }

    private fun renewUiConfig() {
        val json = JSONObject(FileUtil.UI_CONFIG_FILE.readText())
        val starJson = json.getJSONObject("user_star_tab")
        val starJsonList = starJson.getJSONArray("item_list")
        val appIdList = mutableListOf<Pair<String, Long>>()
        for (i in 0 until starJsonList.length()) {
            val item = starJsonList.getJSONObject(i)
            val s = item.getJSONArray("app_id_list").getString(0)
            val (type, id) = s.split("-", limit = 2)
            appIdList.add(Pair(type, id.toLong()))
        }

        val newStarJson = JSONArray()
        for (appId in appIdList) {
            when (appId.first) {
                "app" -> {
                    val item = appDatabaseMap[appId.second] ?: continue
                    newStarJson.put(item.getJSONObject("app_id"))
                }
            }
        }
        val newJson = JSONObject().apply {
            put("user_star_app_id_list", starJson)
        }
        FileUtil.UI_CONFIG_FILE.writeText(
                Gson().toJson(newJson)
        )
    }

    private fun checkHubDatabase() {
        for ((_, json) in appDatabaseMap) {
            val hubUuid = json.getString("hub_uuid")
            hubDatabaseMap[hubUuid] = allHubDatabaseMap[hubUuid] ?: continue
        }
        for ((_, json) in applicationDatabaseMap) {
            val hubUuid = json.getString("hub_uuid")
            hubDatabaseMap[hubUuid] = allHubDatabaseMap[hubUuid] ?: continue
        }
    }

    private fun renewAppId() {
        for ((id, json) in appDatabaseMap) {
            val appIdMap = urlToAppId(json)
            if (appIdMap == null) {
                appDatabaseMap.remove(id)
                continue
            }
            val appIdJson = JSONObject().apply {
                for ((k, v) in appIdMap) {
                    put(k, v)
                }
            }
            json.put("app_id", appIdJson)
        }
    }

    private fun urlToAppId(appJson: JSONObject): Map<String, String?>? {
        val hubUuid = appJson.getString("hub_uuid")
        val hubJson = allHubDatabaseMap[hubUuid] ?: return null
        val urlTemplates = hubJson.getJSONArray("app_url_templates")
        for (i in 0 until urlTemplates.length()) {
            val urlTemp = urlTemplates.getString(i)
            val keyList = AutoTemplate.getArgsKeywords(urlTemp)
            val autoTemplate = AutoTemplate(appJson.getString("url"), urlTemp)
            val args = autoTemplate.args
            if (args.keys == keyList) {
                return args
            }
        }
        return null
    }

    private fun renewIgnoreVersionNumber(applicationJson: JSONObject) {
        val oldJsonArray = applicationJson.getJSONArray("ignore_app_id_list")
        for (i in 0 until oldJsonArray.length()) {
            val ignoreAppJson = oldJsonArray.getJSONObject(i)
            val versionNumber = ignoreAppJson.getOrNull("version_number") ?: continue
            if (versionNumber != "FOREVER_IGNORE") {
                val appIdJson = ignoreAppJson.getJSONObject("package_id")
                ignoreVersionNumberMap[appIdJson] = versionNumber
            }
        }
    }

    private fun renewInvalidPackageList(applicationJson: JSONObject) {
        val oldJsonArray = JSONArray(applicationJson.getOrNull("invalid_package_list") ?: return)
        val hubJson = hubDatabaseMap[applicationJson.getString("hub_uuid")] ?: return
        val jsonArray = JSONArray().apply {
            for (i in 0 until oldJsonArray.length()) {
                val appIdJson = oldJsonArray.getJSONObject(i)
                put(cleanAppId(appIdJson, hubJson) ?: continue)
            }
        }
        applicationJson.put("invalid_package_list", jsonArray)
    }

    private fun renewIgnoreAppIdList(applicationJson: JSONObject) {
        val oldJsonArray = JSONArray(applicationJson.getOrNull("ignore_app_id_list") ?: return)
        val hubJson = hubDatabaseMap[applicationJson.getString("hub_uuid")] ?: return
        val jsonArray = JSONArray().apply {
            for (i in 0 until oldJsonArray.length()) {
                val ignoreAppJson = oldJsonArray.getJSONObject(i)
                if (ignoreAppJson.getOrNull("version_number") == "FOREVER_IGNORE") {
                    val appIdJson = ignoreAppJson.getJSONObject("package_id")
                    put(cleanAppId(appIdJson, hubJson) ?: continue)
                }
            }
        }
        applicationJson.put("auto_ignore_app_id_list", jsonArray)
    }

    private fun cleanAppId(appIdJson: JSONObject, hubJson: JSONObject): JSONObject? {
        val key = mutableListOf<String>().apply {
            val json = hubJson.getJSONObject("hub_config").getJSONArray("api_keywords")
            for (i in 0 until json.length())
                add(json.getString(i))
        }
        val list = mutableListOf<String>().apply {
            for (k in appIdJson.keys()) {
                add(k)
            }
        }
        return if (list.containsAll(key))
            JSONObject().apply {
                for (k in key) {
                    put(k, appIdJson.getOrNull(k))
                }
            } else null
    }

    private fun getOldDatabase(database: SupportSQLiteDatabase) {
        with(database.query("SELECT * FROM app")) {
            while (moveToNext()) {
                val id = getLong(getColumnIndex("id"))
                val name = getString(getColumnIndex("name"))
                val url = getString(getColumnIndex("url"))
                val hubUuid = getString(getColumnIndex("hub_uuid"))
                val ignoreVersionNumber = getStringOrNull(getColumnIndex("ignore_version_number"))
                val cloudConfig = getStringOrNull(getColumnIndex("cloud_config"))
                val appJson = JSONObject().apply {
                    put("name", name)
                    put("url", url)
                    put("hub_uuid", hubUuid)
                    put("ignore_version_number", ignoreVersionNumber)
                    put("cloud_config", cloudConfig)
                }
                appDatabaseMap[id] = appJson
            }
        }
        with(database.query("SELECT * FROM applications")) {
            while (moveToNext()) {
                val id = getLong(getColumnIndex("id"))
                val name = getString(getColumnIndex("name"))
                val hubUuid = getString(getColumnIndex("hub_uuid"))
                val invalidPackageList = getStringOrNull(getColumnIndex("invalid_package_list"))
                val ignoreApps = getStringOrNull(getColumnIndex("ignore_app_list"))
                val json = JSONObject().apply {
                    put("name", name)
                    put("hub_uuid", hubUuid)
                    put("invalid_package_list", invalidPackageList)
                    put("ignore_app_list", ignoreApps)
                }
                applicationDatabaseMap[id] = json
            }
        }
        with(database.query("SELECT * FROM hub")) {
            while (moveToNext()) {
                val uuid = getString(getColumnIndex("uuid"))
                val hubConfig = JSONObject(getString(getColumnIndex("hub_config")))
                allHubDatabaseMap[uuid] = hubConfig
            }
        }
    }
}