package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.utils.StringEncoder
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.Assets
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.dom4j.io.SAXReader

class FDroid(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy,
    private val url: String = "https://f-droid.org/repo"
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "6a6d590b-1809-41bf-8ce3-7e3f6c8da945"

    override fun getRelease(data: ApiRequestData): List<ReleaseGson>? {
        val xmlUrl = getXmlUrl(url)
        val xmlStr = dataCache.get(
            xmlUrl, StringEncoder
        ) { okhttpProxy.okhttpExecute(HttpRequestData(xmlUrl))?.body?.string() ?: return@get null }
            ?: return null
        val appPackage = data.appId[ANDROID_APP_TYPE] ?: return emptyList()
        val root = SAXReader().read(xmlStr.byteInputStream()).rootElement
        val node = root.selectSingleNode(".//application[@id=\"$appPackage\"]")
        var changelog = node.valueOf("changelog")
        return node.selectNodes("package").map {
            ReleaseGson(
                it.valueOf("version"),
                changelog.apply { changelog = null },
                listOf(
                    it.valueOf("apkname").let { name ->
                        Assets(
                            name, "application/vnd.android.package-archive",
                            "$url/$name"
                        )
                    }
                )
            )
        }
    }

    private fun getXmlUrl(url: String) = "$url/index.xml"
}