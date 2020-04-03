package net.xzos.upgradeall.core.server_manager.module.web_api

object WebApiManager {
    private val webApiMap: MutableMap<String, WebApi> = mutableMapOf()

    fun getWebApi(hubUuid: String): WebApi {
        return webApiMap[hubUuid] ?: WebApi(hubUuid).also {
            webApiMap[hubUuid] = it
        }
    }
}