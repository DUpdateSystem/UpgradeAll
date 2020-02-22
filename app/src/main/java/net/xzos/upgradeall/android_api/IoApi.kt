package net.xzos.upgradeall.android_api

import net.xzos.upgradeall.data.json.gson.AppConfigGson
import net.xzos.upgradeall.system_api.interfaces.IoApi
import net.xzos.upgradeall.utils.VersioningUtils
import net.xzos.upgradeall.utils.network.AriaDownloader


// 平台相关 IO 互操作
object IoApi : IoApi {

    init {
        net.xzos.upgradeall.system_api.api.IoApi.ioApiInterface = this
    }

    // 注释相应平台的下载软件
    override fun downloadFile(isDebug: Boolean,
                              fileName: String, url: String, headers: Map<String, String>,
                              externalDownloader: Boolean) {
        AriaDownloader(isDebug).start(fileName, url, headers)
    }

    // 查询软件信息
    override fun getAppVersionNumber(targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?) =
            VersioningUtils.getAppVersionNumber(targetChecker)
}
