package net.xzos.upgradeall.core.system_api.api

import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.server_manager.module.applications.AppInfo
import net.xzos.upgradeall.core.system_api.interfaces.IoApi


// 平台相关 IO 互操作
object IoApi {

    private var ioApiInterface: IoApi? = null

    fun setInterfaces(interfacesClass: IoApi) {
        ioApiInterface = interfacesClass
    }

    // 注释相应平台的下载软件
    internal suspend fun downloadFile(
            fileName: String, url: String, headers: Map<String, String> = mapOf(),
            externalDownloader: Boolean
    ) {
        if (url.isBlank()) return
        ioApiInterface?.downloadFile(fileName, url, headers, externalDownloader)
    }

    // 查询软件信息
    internal fun getAppVersionNumber(targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?): String? {
        return ioApiInterface?.getAppVersionNumber(targetChecker)
    }

    // 获取软件信息列表
    fun getAppInfoList(type: String): List<AppInfo>? {
        return ioApiInterface?.getAppInfoList(type)
    }
}
