package net.xzos.upgradeall.system_api.interfaces

import net.xzos.upgradeall.data.json.gson.AppConfigGson


// 平台相关 IO 互操作
interface IoApi {

    // 注释相应平台的下载软件
    fun downloadFile(isDebug: Boolean,
                     fileName: String, url: String, headers: Map<String, String> = mapOf(),
                     externalDownloader: Boolean)

    // 查询软件信息
    fun getAppVersionNumber(targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?): String?
}
