package net.xzos.upgradeall.android_api

import android.content.pm.PackageManager
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data.json.gson.AppConfigGson
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.server_manager.module.applications.AppInfo
import net.xzos.upgradeall.core.system_api.interfaces.IoApi
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.VersioningUtils
import net.xzos.upgradeall.utils.downloader.AriaDownloader


/**
 * 平台相关 IO 互操作
 */
object IoApi : IoApi {

    init {
        net.xzos.upgradeall.core.system_api.api.IoApi.setInterfaces(this)
    }

    private const val TAG = "IoApi"
    private val objectTag = ObjectTag("Core", TAG)

    private val context = MyApplication.context

    // 注释相应平台的下载软件
    override suspend fun downloadFile(fileName: String, url: String, headers: Map<String, String>,
                              externalDownloader: Boolean) {
        if (!externalDownloader) {
            val ariaDownloader = AriaDownloader(url)
            try {
                ariaDownloader.start(fileName, headers = headers)
            } catch (e: IllegalArgumentException) {
                Log.e(objectTag, TAG, """ downloadFile: 下载任务失败
                        |下载参数: URL: $url, FileName: $fileName, headers: $headers
                        |ERROR_MESSAGE: $e""".trimIndent())
                MiscellaneousUtils.showToast(context, text = "下载失败: $fileName")
            }
        } else {
            MiscellaneousUtils.accessByBrowser(url, context)
        }
    }

    override fun getAppInfoList(type: String): List<AppInfo>? {
        return if (type == AppType.androidApp) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA).map {
                val name = pm.getApplicationLabel(it)
                AppInfo(type, name.toString(), it.packageName)
            }
        } else null
    }

    // 查询软件信息
    override fun getAppVersionNumber(targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?) =
            VersioningUtils.getAppVersionNumber(targetChecker)
}
