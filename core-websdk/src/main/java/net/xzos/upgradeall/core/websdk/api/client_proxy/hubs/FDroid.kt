package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.coroutines.CoroutinesMutableMap
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.websdk.api.client_proxy.APK_CONTENT_TYPE
import net.xzos.upgradeall.core.websdk.api.client_proxy.XmlEncoder
import net.xzos.upgradeall.core.websdk.api.client_proxy.versionCode
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.ApiRequestData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.dom4j.Element
import org.dom4j.io.SAXReader

class FDroid(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "6a6d590b-1809-41bf-8ce3-7e3f6c8da945"

    override fun getAppListRelease(dataList: List<ApiRequestData>): Map<ApiRequestData, List<ReleaseGson>> {
        val map = CoroutinesMutableMap<String, Element>(true)
        return dataList.associateWith { getRelease(it, map) ?: listOf() }
    }

    override fun getRelease(data: ApiRequestData): List<ReleaseGson>? =
        getRelease(data, mutableMapOf())

    private fun getRelease(
        data: ApiRequestData,
        rootMap: MutableMap<String, Element>
    ): List<ReleaseGson>? {
        val appPackage = data.appId[ANDROID_APP_TYPE] ?: return emptyList()
        val url = data.other[REPO_URL] ?: DEF_URL
        val root = rootMap.getOrPut(url) { getRoot(url) ?: return null }
        val node = root.selectSingleNode(".//application[@id=\"$appPackage\"]")
            ?: return emptyList()
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