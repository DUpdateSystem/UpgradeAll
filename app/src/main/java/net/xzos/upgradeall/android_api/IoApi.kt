package net.xzos.upgradeall.android_api

import android.content.pm.PackageManager
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.config.AppType
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.core.server_manager.module.applications.AppInfo
import net.xzos.upgradeall.core.system_api.interfaces.IoApi
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.core.data.json.gson.DownloadInfoItem
import net.xzos.upgradeall.server.downloader.Downloader
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.VersioningUtils


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
    override suspend fun downloadFile(
            downloadInfoList: List<DownloadInfoItem>,
            externalDownloader: Boolean
    ) {
        if (!externalDownloader && !PreferencesMap.enforce_use_external_downloader) {
            try {
                Downloader.startDownloadService(downloadInfoList, context)
            } catch (e: IllegalArgumentException) {
                val name = downloadInfoList[0].name
                Log.e(objectTag, TAG, """ downloadFile: 下载任务失败
                        |下载参数: URL: $name
                        |ERROR_MESSAGE: $e""".trimIndent())
                MiscellaneousUtils.showToast(text = "下载失败: $name")
            }
        } else {
            for (downloadInfo in downloadInfoList) {
                MiscellaneousUtils.accessByBrowser(downloadInfo.url, context)
            }
        }
    }

    // 查询软件信息
    override fun getAppVersionNumber(targetChecker: PackageIdGson?): String? {
        return VersioningUtils.getAppVersionNumber(targetChecker)

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
}
