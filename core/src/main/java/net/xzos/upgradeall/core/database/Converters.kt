package net.xzos.upgradeall.core.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.xzos.upgradeall.core.data.json.AppConfigGson
import net.xzos.upgradeall.core.data.json.HubConfigGson
import net.xzos.upgradeall.core.data.json.IgnoreApp
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableList
import net.xzos.upgradeall.core.utils.coroutines.toCoroutinesMutableList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class Converters {
    @TypeConverter
    fun fromIgnoreAppList(ignoreAppList: List<IgnoreApp>?): String? {
        val list = ignoreAppList?.filter {
            it.packageId.isNotEmpty()
        }
        return if (list.isNullOrEmpty())
            null
        else
            Gson().toJson(list)
    }

    @TypeConverter
    fun stringToIgnoreAppList(s: String?): List<IgnoreApp> {
        if (s.isNullOrEmpty()) return emptyList()
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
        if (s.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<ArrayList<String>?>() {}.type
        return Gson().fromJson(s, listType)
    }

    @TypeConverter
    fun fromAppConfigGson(appConfigGson: AppConfigGson?): String? {
        appConfigGson ?: return null
        return appConfigGson.toString()
    }

    @TypeConverter
    fun stringToAppConfigGson(s: String?): AppConfigGson? {
        if (s.isNullOrEmpty()) return null
        return Gson().fromJson(s, AppConfigGson::class.java)
    }

    @TypeConverter
    fun fromHubConfigGson(hubConfigGson: HubConfigGson): String {
        return hubConfigGson.toString()
    }

    @TypeConverter
    fun stringToHubConfigGson(s: String?): HubConfigGson {
        if (s.isNullOrEmpty()) return HubConfigGson()
        return Gson().fromJson(s, HubConfigGson::class.java)
    }

    private fun stringToCollectionMap(s: String?): Collection<Map<String, String?>> {
        if (s.isNullOrEmpty()) return emptyList()

        return try {
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
            list
        } catch (e: JSONException) {
            emptyList()
        }
    }

    @TypeConverter
    fun stringToCoroutinesMutableListMap(s: String?): CoroutinesMutableList<Map<String, String?>> {
        return stringToCollectionMap(s).toCoroutinesMutableList(true)
    }

    @TypeConverter
    fun fromCoroutinesMutableListMapToString(listMap: CoroutinesMutableList<Map<String, String?>>?): String? {
        return fromCollectionMapToString(listMap)
    }

    @TypeConverter
    fun stringToSetMap(s: String?): HashSet<Map<String, String?>> {
        return stringToCollectionMap(s).toHashSet()
    }

    @TypeConverter
    fun fromListMapToString(listMap: HashSet<Map<String, String?>>?): String? {
        return fromCollectionMapToString(listMap)
    }

    private fun fromCollectionMapToString(listMap: Collection<Map<String, String?>>?): String? {
        if (listMap.isNullOrEmpty()) return null
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
    fun fromMapToString(dict: Map<String, String?>?): String? {
        return JSONObject().apply {
            for ((k, v) in dict?.iterator()?: return null) {
                put(k, v)
            }
        }.toString()
    }

    @TypeConverter
    fun stringToMap(s: String?): MutableMap<String, String?> {
        return mutableMapOf<String, String?>().apply {
            val jsonObject = JSONObject(s?: return mutableMapOf())
            for (k in jsonObject.keys()) {
                this[k] = jsonObject.getString(k)
            }
        }
    }
}