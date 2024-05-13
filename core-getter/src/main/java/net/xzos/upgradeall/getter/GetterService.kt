package net.xzos.upgradeall.getter

import net.xzos.upgradeall.websdk.data.json.ReleaseGson

interface GetterService {
    fun init(dataPath: String, cachePath: String, globalExpireTime: Long): Boolean
    fun checkAppAvailable(hub_uuid: String, app_data: Map<String, String>, hub_data: Map<String, String>): Boolean
    fun getAppLatestRelease(hub_uuid: String, app_data: Map<String, String>, hub_data: Map<String, String>): ReleaseGson
    fun getAppReleases(hub_uuid: String, app_data: Map<String, String>, hub_data: Map<String, String>): List<ReleaseGson>
}