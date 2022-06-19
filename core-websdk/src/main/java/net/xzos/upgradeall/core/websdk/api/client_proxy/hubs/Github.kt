package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.http.OkHttpApi
import net.xzos.upgradeall.core.websdk.api.web.http.openOkHttpApi
import net.xzos.upgradeall.core.websdk.json.Assets
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.json.JSONArray

class Github : BaseHub() {
    private val objectTag = ObjectTag(core, "Github")
    override val uuid = "fd9b2602-62c5-4d55-bd1e-0d6537714ca0"

    override fun getRelease(
        appId: Map<String, String?>,
        auth: Map<String, String?>
    ): List<ReleaseGson>? {
        val owner = appId["owner"]
        val repo = appId["repo"]
        val url = "https://api.github.com/repos/$owner/$repo/releases"
        val requestData = HttpRequestData(url)
        val response = OkHttpApi.callHttpFunc(objectTag, url) {
            openOkHttpApi.getExecute(requestData)
        } ?: return null
        val rawList = JSONArray(response.body.string())
        val data = mutableListOf<ReleaseGson>()
        for (i in 0..rawList.length()) {
            val raw = rawList.getJSONObject(i)
            var name = raw.getString("name")
            if (VersioningUtils.matchVersioningString(name) == null) {
                name = raw.getString("tag_name")
            }
            val assets = mutableListOf<Assets>()
            val rawAssets = raw.getJSONArray("assets")
            for (ai in 0..rawAssets.length()) {
                val rawAsset = rawAssets.getJSONObject(ai)
                val asset = Assets(
                    fileName = rawAsset.getString("name"),
                    fileType = rawAsset.getString("content_type"),
                    downloadUrl = rawAsset.getString("browser_download_url")
                )
                assets.add(asset)
            }
            val release = ReleaseGson(
                versionNumber = name,
                changelog = raw.getString("body"),
                assetList = assets
            )
            data.add(release)
        }
        return data
    }
}