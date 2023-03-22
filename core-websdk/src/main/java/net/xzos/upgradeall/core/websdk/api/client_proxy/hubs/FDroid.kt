package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.websdk.api.client_proxy.APK_CONTENT_TYPE
import net.xzos.upgradeall.core.websdk.api.client_proxy.XmlEncoder
import net.xzos.upgradeall.core.websdk.api.client_proxy.versionCode
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.SingleRequestData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader

class FDroid(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "6a6d590b-1809-41bf-8ce3-7e3f6c8da945"

    override fun checkAppAvailable(data: SingleRequestData): Boolean {
        return getAppNode(data).let { !(it != null && it.second == null) }
    }

    override fun getReleases(data: SingleRequestData): List<ReleaseGson> {
        return getRelease0(data) ?: listOf()
    }

    private fun getRelease0(data: SingleRequestData): List<ReleaseGson>? {
        val (url, node) = getAppNode(data) ?: return null
        node ?: return emptyList()
        var changelog = node.valueOf("changelog")
        return node.selectNodes("package").map {
            ReleaseGson(
                versionNumber = it.valueOf("version"),
                changelog = changelog?.apply { changelog = null },
                assetGsonList = listOf(
                    it.valueOf("apkname").let { name ->
                        AssetGson(name, APK_CONTENT_TYPE, "$url/$name")
                    }
                )
            ).versionCode(it.numberValueOf("versioncode"))
        }
    }

    private fun getAppNode(data: SingleRequestData): Pair<String, Node?>? {
        val url = data.other[REPO_URL] ?: DEF_URL
        val appPackage = data.appId[ANDROID_APP_TYPE] ?: return Pair(url, null)
        val root = getRoot(url) ?: return null
        return Pair(url, root.selectSingleNode(".//application[@id=\"$appPackage\"]"))
    }

    private fun getRoot(url: String): Element? {
        val xmlUrl = getXmlUrl(url)
        val doc = dataCache.get(mutex, xmlUrl, SaveMode.DISK_ONLY, XmlEncoder) {
            val stream = okhttpProxy.okhttpExecute(HttpRequestData(xmlUrl))?.body?.byteStream()
                ?: return@get null
            SAXReader().read(stream)
        } ?: return null
        return doc.rootElement
    }

    private fun getXmlUrl(url: String) = "$url/index.xml"

    companion object {
        private val mutex = Mutex()
        private const val DEF_URL = "https://f-droid.org/repo"

        private const val REPO_URL = "repo_url"
    }
}