package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.asSequence
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils
import net.xzos.upgradeall.core.websdk.api.client_proxy.getAssets
import net.xzos.upgradeall.core.websdk.api.client_proxy.mdToHtml
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.Assets
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.json.JSONArray
import org.json.JSONObject

class Gitlab(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "a84e2fbe-1478-4db5-80ae-75d00454c7eb"

    override fun getRelease(data: ApiRequestData): List<ReleaseGson>? {
        val appId = data.appId
        val owner = appId["owner"]
        val repo = appId["repo"]
        val url = "$GITLAB_HOST/api/v4/projects/$owner%2F$repo/releases"
        val requestData = HttpRequestData(url)
        val response = okhttpProxy.okhttpExecute(requestData)
            ?: return null
        val jsonArray = JSONArray(response.body.string())
        return jsonArray.asSequence<JSONObject>().map { json ->
            val name = data.other[VERSION_NUMBER_KEY]?.let {
                json.getString(it)
            } ?: json.getString("name").let {
                if (VersioningUtils.matchVersioningString(it) == null)
                    json.getString("tag_name")
                else it
            }
            val changelog = json.getString("description").mdToHtml()
            val assetList = json.getJSONObject("assets").getJSONArray("links")
                .asSequence<JSONObject>().map {
                    Assets(
                        fileName = it.getString("name"),
                        fileType = it.getString("link_type"),
                        downloadUrl = it.getString("url")
                    )
                }.toList().ifEmpty { changelog.getAssets(GITLAB_HOST, "$owner/$repo") }
            ReleaseGson(
                versionNumber = name,
                changelog = changelog,
                assetList = assetList
            )
        }.toList()
    }

    companion object {
        private const val VERSION_NUMBER_KEY = "version_number_key"
        private const val GITLAB_HOST = "https://gitlab.com/"
    }
}