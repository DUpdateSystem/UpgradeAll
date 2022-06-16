package net.xzos.upgradeall.core.websdk.proxy.hubs

import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import net.xzos.upgradeall.core.utils.versioning.VersioningUtils
import net.xzos.upgradeall.core.websdk.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.web.http.OkHttpApi
import net.xzos.upgradeall.core.websdk.web.http.openOkHttpApi
import org.json.JSONArray
import org.json.JSONObject

class Github : BaseHub() {
    private val objectTag = ObjectTag(core, "Github")
    override val uuid = "fd9b2602-62c5-4d55-bd1e-0d6537714ca0"

    override fun getRelease(appId: Map<String, String?>, auth: Map<String, String?>): JSONArray? {
        val owner = appId["owner"]
        val repo = appId["repo"]
        val url = "https://api.github.com/repos/$owner/$repo/releases"
        val requestData = HttpRequestData(url)
        val response = OkHttpApi.callHttpFunc(objectTag, url) {
            openOkHttpApi.getExecute(requestData)
        } ?: return null
        val rawList = JSONArray(response.body.string())
        val data = JSONArray()
        for (i in 0..rawList.length()) {
            val raw = rawList.getJSONObject(i)
            val release = JSONObject()
            var name = raw.getString("name")
            if (VersioningUtils.matchVersioningString(name) == null) {
                name = raw.getString("tag_name")
            }
            release.put("version_number", name)
            release.put("change_log", raw.getString("body"))
            val assets = JSONArray()
            val rawAssets = raw.getJSONArray("assets")
            for (ai in 0..rawAssets.length()) {
                val rawAsset = rawAssets.getJSONObject(ai)
                val asset = JSONObject()
                asset.put("file_name", rawAsset.getString("name"))
                asset.put("file_name", rawAsset.getString("browser_download_url"))
                asset.put("file_type", rawAsset.getString("content_type"))
                assets.put(asset)
            }
            release.put("assets", assets)
            data.put(release)
        }
        return data
    }
}