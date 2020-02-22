package net.xzos.upgradeall.system_api.api

import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.system_api.interfaces.IoApi


// 平台相关 IO 互操作
object IoApi {

    var ioApiInterface: IoApi? = null

    // 注释相应平台的下载软件
    fun downloadFile(fileName: String, url: String, headers: Map<String, String> = mapOf(),
                     isDebug: Boolean, externalDownloader: Boolean) {
        ioApiInterface?.downloadFile(isDebug, fileName, url, headers, externalDownloader)
    }

    // 查询软件信息
    fun getAppVersionNumber(targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?): String? {
        return ioApiInterface?.getAppVersionNumber(targetChecker)
    }
}
