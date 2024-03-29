package net.xzos.upgradeall.core.database.migration

import android.annotation.SuppressLint
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.xzos.upgradeall.core.utils.AutoTemplate
import net.xzos.upgradeall.core.utils.cleanBlankValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

// Deprecated
@SuppressLint("SdCardPath")
internal val UI_CONFIG_FILE = File("/data/data/net.xzos.upgradeall/files", "ui.json")

@Suppress("ClassName")
open class Migration_8_9_10_Share(startVersion: Int, endVersion: Int) : Migration(startVersion, endVersion) {
    private val appDatabaseMap = mutableMapOf<Long, JSONObject>()
    private val applicationDatabaseMap = mutableMapOf<Long, JSONObject>()
    private val allHubDatabaseMap = mutableMapOf<String, JSONObject>()
    private val hubDatabaseMap = mutableMapOf<String, JSONObject>()

    private val ignoreVersionNumberMap = mutableMapOf<JSONObject, String>()

    override fun migrate(database: SupportSQLiteDatabase) {
        getOldDatabase(database)
        checkHubDatabase()
        renewAppId()
        renewUiConfig()
        deleteOldDatabase(database)
        renewDatabaseData()
        createNewDatabase(database)
    }

    protected open fun deleteOldDatabase(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX app_key_value")
        database.execSQL("DROP INDEX applications_key_value")
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
            uuid TEXT PRIMARY KEY NOT NULL,
            hub_config TEXT NOT NULL,
            auth TEXT NOT NULL,
            ignore_app_id_list TEXT NOT NULL,
            user_ignore_app_id_list TEXT NOT NULL
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
            val ignoreVersionNumber = appJson.getOrNull("ignore_version_number")
            val ignoreVersionNumberS = if (ignoreVersionNumber == null) null else "'$ignoreVersionNumber'"
            val cloudConfig = appJson.getOrNull("cloud_config")
            val cloudConfigS = if (cloudConfig == null) null else "'$cloudConfig'"
            database.execSQL("""
            INSERT INTO app (name, app_id, ignore_version_number, cloud_config)
            VALUES ('${appJson.getString("name")}', '${appJson.getString("app_id")}', $ignoreVersionNumberS, $cloudConfigS);
            """
            )
        }
        for ((hub_uuid, hubJson) in hubDatabaseMap) {
            database.execSQL("""
            INSERT INTO hub(uuid, hub_config, auth, ignore_app_id_list, user_ignore_app_id_list)
            VALUES ('$hub_uuid', '${hubJson.getString("hub_config")}', '{}', '${hubJson.getOrNull("invalid_package_list") ?: "[]"}', '${hubJson.getOrNull("user_ignore_app_id_list") ?: "[]"}');
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
        val starJson = try {
            val json = JSONObject(UI_CONFIG_FILE.readText())
            json.getJSONObject("user_star_tab")
        } catch (ignore: Throwable) {
            return
        }
        val starJsonList = starJson.getJSONArray("item_list")
        val appIdList = mutableListOf<Pair<String, Long>>()
        for (i in 0 until starJsonList.length()) {
            val item = starJsonList.getJSONObject(i)
            val s = item.getJSONArray("app_id_list").getString(0)
            try {
                val (type, id) = s.split("-", limit = 2)
                appIdList.add(Pair(type, id.toLong()))
            } catch (ignore: Throwable) {
            }
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
            put("user_star_app_id_list", newStarJson)
        }
        UI_CONFIG_FILE.writeText(newJson.toString())
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
        for ((_, json) in appDatabaseMap.toMap()) {
            val appIdMap = urlToAppId(json)?.plus(packageIdToAppId(json)?.cleanBlankValue()
                    ?: mapOf()) ?: mapOf()
            val appIdJson = JSONObject().apply {
                for ((k, v) in appIdMap) {
                    if (!v.isNullOrBlank()) put(k, v)
                }
            }
            json.put("app_id", appIdJson)
        }
    }

    private fun packageIdToAppId(appJson: JSONObject): Map<String, String>? {
        val packageId = JSONObject(appJson.getString("package_id"))
        val k = packageId.getString("api")
        val v = packageId.getString("extra_string")
        return mapOf(Pair(appIdConverter(k) ?: return null, v))
    }

    private fun appIdConverter(s: String): String? {
        return when (s.toLowerCase(Locale.ENGLISH)) {
            "app_package" -> "android_app_package"
            "magisk_module" -> "android_magisk_module"
            "shell" -> "android_custom_shell"
            "shell_root" -> "android_custom_shell_root"
            else -> null
        }
    }

    private fun urlToAppId(appJson: JSONObject): Map<String, String?>? {
        // AutoTemplate urlToAppId 的拷贝，避免后续架构升级导致数据库升级逻辑错误
        val url = appJson.getString("url") ?: return null
        val hubUuid = appJson.getString("hub_uuid")
        allHubDatabaseMap[hubUuid]?.let {
            getAppIdFromUrl(url, it)?.run { return this }
        }
        allHubDatabaseMap.values.forEach {
            getAppIdFromUrl(url, it)?.run { return this }
        }
        return null
    }

    private fun getAppIdFromUrl(url: String, hubJson: JSONObject): Map<String, String?>? {
        val urlTemplates = hubJson.getJSONObject("hub_config").getJSONArray("app_url_templates")
        for (i in 0 until urlTemplates.length()) {
            val urlTemp = urlTemplates.getString(i)
            val autoTemplate = AutoTemplate(url, urlTemp)
            if (autoTemplate.checkFull()) {
                return autoTemplate.args.mapKeys { it.key.replaceFirst("%", "") }
            }
        }
        return null
    }

    private fun renewIgnoreVersionNumber(applicationJson: JSONObject) {
        val oldJsonArray = JSONArray(applicationJson.getOrNull("ignore_app_list") ?: return)
        val hubJson = hubDatabaseMap[applicationJson.getString("hub_uuid")] ?: return
        for (i in 0 until oldJsonArray.length()) {
            val ignoreAppJson = oldJsonArray.getJSONObject(i)
            val versionNumber = ignoreAppJson.getOrNull("version_number") ?: continue
            if (versionNumber != "FOREVER_IGNORE") {
                val appIdJsonS = ignoreAppJson.getOrNull("package_id") ?: continue
                val appIdJson = JSONObject(appIdJsonS)
                ignoreVersionNumberMap[cleanAppId(appIdJson, hubJson.getJSONObject("hub_config"))
                        ?: continue] = versionNumber
            }
        }
    }

    private fun renewInvalidPackageList(applicationJson: JSONObject) {
        val oldJsonArray = JSONArray(applicationJson.getOrNull("invalid_package_list") ?: return)
        val hubJson = hubDatabaseMap[applicationJson.getString("hub_uuid")] ?: return
        val jsonArray = JSONArray().apply {
            for (i in 0 until oldJsonArray.length()) {
                val appIdJson = oldJsonArray.getJSONObject(i)
                put(cleanAppId(appIdJson, hubJson.getJSONObject("hub_config")) ?: continue)
            }
        }
        hubJson.put("invalid_package_list", jsonArray)
        hubDatabaseMap[applicationJson.getString("hub_uuid")] = hubJson
    }

    private fun renewIgnoreAppIdList(applicationJson: JSONObject) {
        val oldJsonArray = JSONArray(applicationJson.getOrNull("ignore_app_list") ?: return)
        val hubJson = hubDatabaseMap[applicationJson.getString("hub_uuid")] ?: return
        val jsonArray = JSONArray().apply {
            for (i in 0 until oldJsonArray.length()) {
                val ignoreAppJson = oldJsonArray.getJSONObject(i)
                if (ignoreAppJson.getOrNull("version_number") == "FOREVER_IGNORE") {
                    val appIdJsonS = ignoreAppJson.getOrNull("package_id") ?: continue
                    val appIdJson = JSONObject(appIdJsonS)
                    put(cleanAppId(appIdJson, hubJson.getJSONObject("hub_config")) ?: continue)
                }
            }
        }
        hubJson.put("user_ignore_app_id_list", jsonArray)
        hubDatabaseMap[applicationJson.getString("hub_uuid")] = hubJson
    }

    private fun cleanAppId(appIdJson: JSONObject, hubJson: JSONObject): JSONObject? {
        val key = mutableListOf<String>().apply {
            val json = hubJson.getJSONArray("api_keywords")
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
                val id = getLong(getColumnIndexOrThrow("id"))
                val name = getString(getColumnIndexOrThrow("name"))
                val url = getString(getColumnIndexOrThrow("url"))
                val packageId = getString(getColumnIndexOrThrow("package_id"))
                val hubUuid = getString(getColumnIndexOrThrow("hub_uuid"))
                val ignoreVersionNumber = getStringOrNull(getColumnIndex("ignore_version_number"))
                val cloudConfig = getStringOrNull(getColumnIndex("cloud_config"))
                val appJson = JSONObject().apply {
                    put("name", name)
                    put("url", url)
                    put("package_id", packageId)
                    put("hub_uuid", hubUuid)
                    put("ignore_version_number", ignoreVersionNumber ?: JSONObject.NULL)
                    put("cloud_config", cloudConfig ?: JSONObject.NULL)
                }
                appDatabaseMap[id] = appJson
            }
        }
        with(database.query("SELECT * FROM applications")) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow("id"))
                val name = getString(getColumnIndexOrThrow("name"))
                val hubUuid = getString(getColumnIndexOrThrow("hub_uuid"))
                val invalidPackageList = getStringOrNull(getColumnIndex("invalid_package_list"))
                val ignoreApps = getStringOrNull(getColumnIndex("ignore_app_list"))
                val json = JSONObject().apply {
                    put("name", name)
                    put("hub_uuid", hubUuid)
                    put("invalid_package_list", invalidPackageList ?: JSONObject.NULL)
                    put("ignore_app_list", ignoreApps ?: JSONObject.NULL)
                }
                applicationDatabaseMap[id] = json
            }
        }
        with(database.query("SELECT * FROM hub")) {
            while (moveToNext()) {
                val uuid = getString(getColumnIndexOrThrow("uuid"))
                val hubConfig = JSONObject(getString(getColumnIndexOrThrow("hub_config")))
                allHubDatabaseMap[uuid] = JSONObject().apply { put("hub_config", hubConfig) }
            }
        }
    }
}

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
private fun JSONObject.getOrNull(key: String): String? {
    return if (this.has("key"))
        with(this.getString(key)) {
            if (this != "null")
                this
            else null
        } else null
}

val MIGRATION_8_9 = object : Migration_8_9_10_Share(8, 9) {
    override fun deleteOldDatabase(database: SupportSQLiteDatabase) {
        super.deleteOldDatabase(database)
        database.execSQL("CREATE UNIQUE INDEX app_key_value on app (app_id)")
    }
}