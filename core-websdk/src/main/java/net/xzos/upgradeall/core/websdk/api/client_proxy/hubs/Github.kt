package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.asSequence
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.json.Assets
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.json.JSONArray
import org.json.JSONObject

internal class Github(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid = "fd9b2602-62c5-4d55-bd1e-0d6537714ca0"

    override fun getRelease(
        appId: Map<String, String?>,
        auth: Map<String, String?>
    ): List<ReleaseGson>? {
        val owner = appId["owner"]
        val repo = appId["repo"]
        val url = "https://api.github.com/repos/$owner/$repo/releases"
        val requestData = HttpRequestData(url)
        val response = okhttpProxy.okhttpExecute(requestData)
            ?: return null
        val jsonArray = JSONArray(response.body.string())
        val data = jsonArray.asSequence<JSONObject>().map {
            var name = it.getString("name")
            if (VersioningUtils.matchVersioningString(name) == null) {
                name = it.getString("tag_name")
            }
            ReleaseGson(
                versionNumber = name,
                changelog = it.getString("body"),
                assetList = it.getJSONArray("assets").asSequence<JSONObject>().map { assets ->
                    Assets(
                        fileName = assets.getString("name"),
                        fileType = assets.getString("content_type"),
                        downloadUrl = assets.getString("browser_download_url")
                    )
                }.toList()
            )
        }.toList()
        return data
    }
}