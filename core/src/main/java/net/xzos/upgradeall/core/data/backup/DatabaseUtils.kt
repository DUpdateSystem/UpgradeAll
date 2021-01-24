package net.xzos.upgradeall.core.data.backup

import com.google.gson.Gson
import net.xzos.upgradeall.core.database.Converters
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity
import org.json.JSONObject
import java.security.MessageDigest

private val converters = Converters()
fun AppEntity.toJson(): JSONObject {
    val converters = Converters()
    return JSONObject(mapOf(
            "name" to name,
            "app_id" to converters.fromMapToString(appId),
            "ignore_version_number" to ignoreVersionNumber,
            "cloud_config" to Gson().toJson(cloudConfig)
    ))
}

fun AppEntity.md5(): String {
    val key = name + appId
    val md = MessageDigest.getInstance("MD5")
    md.update(key.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun HubEntity.toJson(): JSONObject {
    return JSONObject(mapOf(
            "uuid" to uuid,
            "hub_config" to Gson().toJson(hubConfig),
            "auth" to converters.fromMapToString(auth),
            "ignore_app_id_list" to converters.fromListMapToString(ignoreAppIdList),
    ))
}

fun HubEntity.md5(): String {
    val key = uuid
    val md = MessageDigest.getInstance("MD5")
    md.update(key.toByteArray())
    return md.digest().toString(Charsets.UTF_8)
}

fun parseAppEntityConfig(json: JSONObject): AppEntity {
    return AppEntity(
            0,
            json.getString("name"),
            converters.stringToMap(json.getString("app_id")),
            json.getOrNull("ignore_version_number"),
            converters.stringToAppConfigGson(json.getString("cloud_config")),
    )
}

fun parseHubEntityConfig(json: JSONObject): HubEntity{
    return HubEntity(
            json.getString("uuid"),
            converters.stringToHubConfigGson(json.getString("hub_config")),
            converters.stringToMap(json.getOrNull("auth")),
            converters.stringToSetMap(json.getOrNull("ignore_app_id_list")),
    )
}

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
fun JSONObject.getOrNull(key: String): String? = with(this.getString(key)) {
    if (this != "null")
        this
    else null
}
