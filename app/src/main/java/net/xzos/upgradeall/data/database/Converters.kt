package net.xzos.upgradeall.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.gson.HubConfigGson
import net.xzos.upgradeall.core.data.json.gson.IgnoreApp
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromIgnoreAppList(ignoreAppList: List<IgnoreApp>): String? {
        val list = ignoreAppList.filter {
            it.packageId.isNotEmpty() && it.versionNumber != null
        }
        return if (list.isEmpty())
            null
        else
            Gson().toJson(list)
    }

    @TypeConverter
    fun stringToIgnoreAppList(s: String?): List<IgnoreApp> {
        if (s == null) return emptyList()
        val listType = object : TypeToken<ArrayList<IgnoreApp>?>() {}.type
        return Gson().fromJson(s, listType)
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String? {
        return if (list.isEmpty())
            null
        else
            Gson().toJson(list)
    }

    @TypeConverter
    fun stringToList(s: String?): List<String> {
        if (s == null) return emptyList()
        val listType = object : TypeToken<ArrayList<String>?>() {}.type
        return Gson().fromJson(s, listType)
    }

    @TypeConverter
    fun fromAppConfigGson(appConfigGson: AppConfigGson): String? {
        return Gson().toJson(appConfigGson)
    }

    @TypeConverter
    fun stringToAppConfigGson(s: String?): AppConfigGson? {
        if (s == null) return null
        return Gson().fromJson(s, AppConfigGson::class.java)
    }

    @TypeConverter
    fun fromPackageId(packageId: PackageIdGson): String {
        return Gson().toJson(packageId)
    }

    @TypeConverter
    fun stringToPackageId(s: String?): PackageIdGson {
        if (s == null) return PackageIdGson()
        return Gson().fromJson(s, PackageIdGson::class.java)
    }

    @TypeConverter
    fun stringToListMap(s: String?): List<Map<String, String>> {
        if (s == null) return emptyList()
        val jsonArray = JSONArray(s)
        val list = mutableListOf<Map<String, String>>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val map = mutableMapOf<String, String>()
            for (k in jsonObject.keys()) {
                map[k] = jsonObject.getString(k)
            }
            list.add(map)
        }
        return list
    }

    @TypeConverter
    fun listMapToString(listMap: List<Map<String, String>>): String? {
        if (listMap.isEmpty()) return null
        val jsonArray = JSONArray()
        for (map in listMap) {
            val jsonObject = JSONObject()
            for ((k, v) in map.iterator()) {
                jsonObject.put(k, v)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun fromMap(dict: Map<String, String?>): String? {
        if (dict.isEmpty()) return null
        val jsonObject = JSONObject()
        for ((k, v) in dict.entries) {
            jsonObject.put(k, v)
        }
        return jsonObject.toString()
    }

    @TypeConverter
    fun stringToMap(s: String?): Map<String, String?> {
        if (s == null) return mapOf()
        val jsonObject = JSONObject(s)
        val map = mutableMapOf<String, String?>()
        for (k in jsonObject.keys()) {
            map[k] = jsonObject.getString(k)
        }
        return map
    }

    @TypeConverter
    fun fromHubConfigGson(hubConfigGson: HubConfigGson): String {
        return Gson().toJson(hubConfigGson)
    }

    @TypeConverter
    fun stringToHubConfigGson(s: String?): HubConfigGson {
        if (s == null) return HubConfigGson()
        return Gson().fromJson(s, HubConfigGson::class.java)
    }
}
