package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.asSequence
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.utils.data_cache.utils.JsonArrayEncoder
import net.xzos.upgradeall.core.utils.getOrNull
import net.xzos.upgradeall.core.utils.iterator
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.json.JSONArray
import org.json.JSONObject

class LsposedRepo(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "401e6259-2eab-46f0-8e8a-d2bfafedf5bf"
    override fun checkAppAvailable(data: SingleRequestData): Boolean? {
        return getAppJson(data)?.length() != 0
    }

    override fun getReleases(data: SingleRequestData): List<ReleaseGson>? {
        val json = getAppJson(data) ?: return null
        val jsonArray = json.getOrNull("releases", json::getJSONArray) ?: return emptyList()
        return jsonArray.asSequence<JSONObject>().map {
            ReleaseGson(
                versionNumber = it.getString("name"),
                changelog = it.getOrNull("descriptionHTML"),
                assetGsonList = it.getJSONArray("releaseAssets").asSequence<JSONObject>()
                    .map { asset ->
                        AssetGson(
                            fileName = asset.getString("name"),
                            fileType = asset.getString("contentType"),
                            downloadUrl = asset.getString("downloadUrl")
                        )
                    }.toList()
            )
        }.toList()
    }

    private fun getAppJson(data: SingleRequestData): JSONObject? {
        val dataJson = dataCache.get(
            mutex,
            "lsposed_repo_json", SaveMode.DISK_ONLY, JsonArrayEncoder,
            false
        ) {
            okhttpProxy.okhttpExecute(
                HttpRequestData("https://modules.lsposed.org/modules.json")
            )?.let {
                JSONArray(it.body.string())
            }
        } ?: return null

        val appPackage = data.appId[ANDROID_APP_TYPE]
        var json: JSONObject? = null
        for (tmp in dataJson.iterator<JSONObject>()) {
            if (tmp.getString("name") == appPackage) {
                json = tmp
                break
            }
        }
        return json ?: JSONObject()
    }

    companion object {
        private val mutex = Mutex()
    }
}