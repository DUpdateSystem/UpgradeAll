package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.utils.constant.ANDROID_APP_TYPE
import net.xzos.upgradeall.core.utils.coroutines.ValueMutex
import net.xzos.upgradeall.core.utils.data_cache.DataCacheManager
import net.xzos.upgradeall.core.utils.data_cache.cache_object.SaveMode
import net.xzos.upgradeall.core.websdk.api.client_proxy.APK_CONTENT_TYPE
import net.xzos.upgradeall.core.websdk.api.client_proxy.XmlEncoder
import net.xzos.upgradeall.core.websdk.api.client_proxy.versionCode
import net.xzos.upgradeall.core.websdk.api.web.http.HttpRequestData
import net.xzos.upgradeall.core.websdk.api.web.proxy.OkhttpProxy
import net.xzos.upgradeall.core.websdk.base_model.AppData
import net.xzos.upgradeall.core.websdk.base_model.HubData
import net.xzos.upgradeall.core.websdk.json.AssetGson
import net.xzos.upgradeall.core.websdk.json.ReleaseGson
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader

class FDroid(
    dataCache: DataCacheManager, okhttpProxy: OkhttpProxy
) : BaseHub(dataCache, okhttpProxy) {
    override val uuid: String = "6a6d590b-1809-41bf-8ce3-7e3f6c8da945"

    override fun checkAppAvailable(hub: HubData, app: AppData): Boolean {
        return getReleases(hub, app).isNotEmpty()
    }

    override fun getUpdate(
        hub: HubData, appList: Collection<AppData>
    ): Map<AppData, ReleaseGson?>? {
        val url = hub.other[REPO_URL] ?: DEF_URL
        val root = getRoot(url) ?: return null
        return appList.associateWith { getRelease(root, it).firstOrNull() }
    }

    override fun getReleases(hub: HubData, app: AppData): List<ReleaseGson> {
        val url = hub.other[REPO_URL] ?: DEF_URL
        val root = getRoot(url) ?: return emptyList()
        return getRelease(root, app)
    }

    private fun getRelease(root: Element, app: AppData): List<ReleaseGson> {
        val url = root.valueOf("//repo//url")
        val appPackage = app.appId[ANDROID_APP_TYPE] ?: return emptyList()
        val node = root.selectSingleNode(".//application[@id=\"$appPackage\"]")
        return getRelease(url, node)
    }

    private fun getRelease(url: String, node: Node): List<ReleaseGson> {
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
        val doc = dataCache.get(mutex, SaveMode.MEMORY_AND_DISK, xmlUrl, XmlEncoder) {
            val stream = okhttpProxy.okhttpExecute(HttpRequestData(xmlUrl))?.body?.byteStream()
                ?: return@get null
            SAXReader().read(stream)
        } ?: return null
        return doc.rootElement
    }

    private fun getXmlUrl(url: String) = "$url/index.xml"

    companion object {
        private val mutex = ValueMutex()
        private const val DEF_URL = "https://f-droid.org/repo"

        private const val REPO_URL = "repo_url"
    }
}