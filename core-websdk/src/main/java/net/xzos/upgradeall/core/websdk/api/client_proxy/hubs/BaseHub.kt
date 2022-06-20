package net.xzos.upgradeall.core.websdk.api.client_proxy.hubs

import net.xzos.upgradeall.core.websdk.json.Assets
import net.xzos.upgradeall.core.websdk.json.DownloadItem
import net.xzos.upgradeall.core.websdk.json.ReleaseGson

abstract class BaseHub {
    abstract val uuid: String

    abstract fun getRelease(
        appId: Map<String, String?>,
        auth: Map<String, String?>
    ): List<ReleaseGson>?

    open fun getDownload(
        appId: Map<String, String?>,
        auth: Map<String, String?>,
        assetIndex: List<Int>,
        assets: Assets?
    ): List<DownloadItem>? {
        return null
    }
}