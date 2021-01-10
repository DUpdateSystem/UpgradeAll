package net.xzos.upgradeall.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.xzos.upgradeall.core.data.json.UIConfig
import net.xzos.upgradeall.core.data.json.changeAppDatabaseId
import net.xzos.upgradeall.core.data.json.save
import org.json.JSONArray
import org.json.JSONObject

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE app
            (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            hub_uuid TEXT NOT NULL,
            url TEXT NOT NULL,
            auth TEXT DEFAULT null,
            extra_id TEXT DEFAULT null,
            package_id TEXT DEFAULT null,
            ignore_version_number TEXT DEFAULT null,
            cloud_config TEXT DEFAULT null,
            unique (name, hub_uuid, url, package_id)
            );
        """
        )

        database.execSQL(
            """
           CREATE UNIQUE INDEX app_key_value
           on app (name, hub_uuid, url, package_id); 
        """
        )

        database.execSQL(
            """
            CREATE TABLE applications
            (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            hub_uuid TEXT NOT NULL,
            auth TEXT DEFAULT null,
            extra_id TEXT DEFAULT null,
            ignore_app_list TEXT DEFAULT null,
            invalid_package_list TEXT DEFAULT null,
            unique (hub_uuid)
            );
        """
        )

        database.execSQL(
            """
           CREATE UNIQUE INDEX applications_key_value
           on applications (hub_uuid); 
        """
        )

        database.execSQL(
            """
            CREATE TABLE hub
            (
            uuid TEXT PRIMARY KEY NOT NULL,
            hub_config TEXT NOT NULL
            );
        """
        )
        with(database.query("SELECT * FROM hubdatabase")) {
            while (moveToNext()) {
                val uuid = getString(getColumnIndex("uuid"))
                val hubConfig = getString(getColumnIndex("hub_config"))
                database.execSQL(
                    """
                    INSERT INTO hub (uuid, hub_config)
                    VALUES ('$uuid', '$hubConfig');
                """
                )
            }
        }
        var appDatabaseIndex = 0
        var applicationsDatabaseIndex = 0
        val databaseIdMap = mutableMapOf<String, String>()
        with(database.query("SELECT * FROM repodatabase")) {
            while (moveToNext()) {
                val id = getLong(getColumnIndex("id"))
                val type = getString(getColumnIndex("type"))
                val name = getString(getColumnIndex("name"))
                val hubUuid = getString(getColumnIndex("api_uuid"))
                val extraData = try {
                    JSONObject(getString(getColumnIndex("extra_data")))
                } catch (e: Throwable) {
                    null
                }
                if (type == "applications") {
                    applicationsDatabaseIndex += 1
                    databaseIdMap[id.toString()] = "applications-$applicationsDatabaseIndex"
                    var invalidPackageList: String? = null
                    var ignoreApps: String? = null
                    try {
                        val invalidPackageListJson = extraData?.getJSONObject("applications_config")
                            ?.getJSONArray("invalid_package_name")!!
                        val json = JSONArray()
                        for (i in 0 until invalidPackageListJson.length()) {
                            val p = invalidPackageListJson.getString(i)
                            json.put(JSONObject().apply {
                                put("android_app_package", p)
                            })
                        }
                        invalidPackageList = json.toString()
                    } catch (e: Throwable) {
                    }
                    try {
                        val ignoreAppsJson = extraData?.getJSONObject("applications_config")
                            ?.getJSONArray("ignore_apps")!!
                        for (i in 0 until ignoreAppsJson.length()) {
                            val item = ignoreAppsJson.getJSONObject(i)
                            val packageId = item.getString("package")
                            item.put("package_id", packageId)
                            if (item.getBoolean("forever"))
                                item.put("version_number", "FOREVER_IGNORE")
                        }
                        ignoreApps = ignoreAppsJson.toString()
                    } catch (e: Throwable) {
                    }
                    try {
                        val ignoreAppsJson = extraData?.getJSONObject("applications_config")
                            ?.getJSONArray("ignore_apps")!!
                        for (i in 0 until ignoreAppsJson.length()) {
                            val item = ignoreAppsJson.getJSONObject(i)
                            if (item.getBoolean("forever"))
                                item.put("version_number", "FOREVER_IGNORE")
                        }
                        ignoreApps = ignoreAppsJson.toString()
                    } catch (e: Throwable) {
                    }
                    val invalidPackageListString =
                        if (invalidPackageList != null) "'$invalidPackageList'" else null
                    val ignoreAppsString = if (ignoreApps != null) "'$ignoreApps'" else null
                    database.execSQL(
                        """
                    INSERT INTO applications (name, hub_uuid, auth, extra_id, invalid_package_list, ignore_app_list)
                    VALUES ('$name', '$hubUuid', null, null, $invalidPackageListString, $ignoreAppsString)
                    """
                    )
                } else {
                    appDatabaseIndex += 1
                    databaseIdMap[id.toString()] = "app-$appDatabaseIndex"
                    val url = getString(getColumnIndex("url"))
                    val packageId = getString(getColumnIndex("versionchecker"))
                    var ignoreVersionNumber: String? = null
                    var cloudConfig: String? = null
                    try {
                        ignoreVersionNumber = extraData?.getString("mark_processed_version_number")
                        cloudConfig = extraData?.getString("cloud_app_config")
                    } catch (e: Throwable) {
                    }
                    val ignoreVersionNumberString =
                        if (ignoreVersionNumber != null) "'$ignoreVersionNumber'" else null
                    val cloudConfigString = if (cloudConfig != null) "'$cloudConfig'" else null
                    val packageIdString = if (packageId != null) "'$packageId'" else null
                    database.execSQL(
                        """
                    INSERT INTO app (name, hub_uuid, auth, extra_id, url, package_id, ignore_version_number, cloud_config)
                    VALUES ('$name', '$hubUuid', null, null, '$url', $packageIdString, $ignoreVersionNumberString, $cloudConfigString);
                    """
                    )
                }
            }
        }
        with(UIConfig.uiConfig) {
            changeAppDatabaseId(databaseIdMap)
            save()
        }
        database.execSQL("DROP TABLE android_metadata")
        database.execSQL("DROP TABLE repodatabase")
        database.execSQL("DROP TABLE hubdatabase")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX applications_key_value")
        database.execSQL(
            """
           CREATE UNIQUE INDEX applications_key_value
           on applications (hub_uuid, extra_id, auth); 
        """
        )
    }
}