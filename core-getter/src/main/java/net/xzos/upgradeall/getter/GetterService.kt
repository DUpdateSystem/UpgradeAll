package net.xzos.upgradeall.getter

import com.googlecode.jsonrpc4j.JsonRpcMethod
import com.googlecode.jsonrpc4j.JsonRpcParam
import net.xzos.upgradeall.websdk.data.json.CloudConfigList
import net.xzos.upgradeall.websdk.data.json.ReleaseGson

interface GetterService {
    @JsonRpcMethod("ping")
    fun ping(): String

    @JsonRpcMethod("init")
    fun init(
        @JsonRpcParam(value = "data_path") dataPath: String,
        @JsonRpcParam(value = "cache_path") cachePath: String,
        @JsonRpcParam(value = "global_expire_time") globalExpireTime: Long
    ): Boolean

    @JsonRpcMethod("shutdown")
    fun shutdown()

    @JsonRpcMethod("check_app_available")
    fun checkAppAvailable(
        @JsonRpcParam(value = "hub_uuid") hubUuid: String,
        @JsonRpcParam(value = "app_data") appData: Map<String, String>,
        @JsonRpcParam(value = "hub_data") hubData: Map<String, String>,
    ): Boolean

    @JsonRpcMethod("get_latest_release")
    fun getAppLatestRelease(
        @JsonRpcParam(value = "hub_uuid") hubUuid: String,
        @JsonRpcParam(value = "app_data") appData: Map<String, String>,
        @JsonRpcParam(value = "hub_data") hubData: Map<String, String>,
    ): ReleaseGson

    @JsonRpcMethod("get_releases")
    fun getAppReleases(
        @JsonRpcParam(value = "hub_uuid") hubUuid: String,
        @JsonRpcParam(value = "app_data") appData: Map<String, String>,
        @JsonRpcParam(value = "hub_data") hubData: Map<String, String>,
    ): List<ReleaseGson>

    @JsonRpcMethod("get_cloud_config")
    fun getCloudConfig(
        @JsonRpcParam(value = "url") url: String
    ): CloudConfigList
}