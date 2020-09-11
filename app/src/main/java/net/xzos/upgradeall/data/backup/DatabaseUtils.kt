package net.xzos.upgradeall.data.backup

import com.google.gson.Gson
import net.xzos.upgradeall.core.data.coroutines_basic_data_type.toCoroutinesMutableList
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.data.database.ApplicationsDatabase
import net.xzos.upgradeall.core.data.database.HubDatabase
import net.xzos.upgradeall.data.database.Converters
import org.json.JSONObject
import java.security.MessageDigest

private val converters = Converters()
fun AppDatabase.toJson(): JSONObject {
    val converters = Converters()
    return JSONObject(mapOf(
            "name" to name,
            "hub_uuid" to hubUuid,
            "auth" to converters.fromMapToString(auth),
            "extra_id" to converters.fromMapToString(extraId),
            "url" to url,
            "package_id" to Gson().toJson(packageId),
            "ignore_version_number" to ignoreVersionNumber,
            "cloud_config" to Gson().toJson(cloudConfig)
    ))
}

fun AppDatabase.md5(): String {
    val key = name + hubUuid + url + packageId
    val md = MessageDigest.getInstance("MD5")
    md.update(key.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun ApplicationsDatabase.toJson(): JSONObject {
    return JSONObject(mapOf(
            "name" to name,
            "hub_uuid" to hubUuid,
            "auth" to converters.fromMapToString(auth),
            "extra_id" to converters.fromMapToString(extraId),
            "invalid_package_list" to converters.fromListMapToString(invalidPackageList),
            "ignore_app_list" to converters.fromIgnoreAppList(ignoreApps)
    ))
}

fun ApplicationsDatabase.md5(): String {
    val key = hubUuid
    val md = MessageDigest.getInstance("MD5")
    md.update(key.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun HubDatabase.toJson(): JSONObject {
    return JSONObject(mapOf(
            "uuid" to uuid,
            "hub_config" to converters.fromHubConfigGson(hubConfig)
    ))
}

fun parseAppDatabaseConfig(json: JSONObject): AppDatabase {
    return AppDatabase(
            0,
            json.getString("name"), json.getString("hub_uuid"), json.getString("url"),
            converters.stringToPackageId(json.getString("package_id")),
            converters.stringToAppConfigGson(json.getString("cloud_config")),
            converters.stringToMap(json.getString("auth")),
            converters.stringToMap(json.getString("extra_id")),
            json.getString("ignore_version_number")
    )
}

fun parseApplicationsDatabaseConfig(json: JSONObject): ApplicationsDatabase {
    return ApplicationsDatabase(
            0,
            json.getString("name"), json.getString("hub_uuid"),
            converters.stringToMap(json.getString("auth")),
            converters.stringToMap(json.getString("extra_id")),
            converters.stringToListMap(json.getString("invalid_package_list")).toCoroutinesMutableList(),
            converters.stringToIgnoreAppList(json.getString("ignore_app_list")).toCoroutinesMutableList()
    )
}

fun parseHubDatabaseConfig(json: JSONObject): HubDatabase {
    return HubDatabase(
            json.getString("uuid"),
            converters.stringToHubConfigGson(json.getString("hub_config"))
    )
}
