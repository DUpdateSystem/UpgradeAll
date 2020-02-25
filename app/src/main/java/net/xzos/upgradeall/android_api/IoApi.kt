package net.xzos.upgradeall.android_api

import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.dupdatesystem.data.json.gson.AppConfigGson
import net.xzos.dupdatesystem.data.json.nongson.ObjectTag
import net.xzos.dupdatesystem.system_api.interfaces.IoApi
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.VersioningUtils
import net.xzos.upgradeall.utils.network.AriaDownloader


/**
 * 平台相关 IO 互操作
 */
object IoApi : IoApi {

    init {
        net.xzos.dupdatesystem.system_api.api.IoApi.ioApiInterface = this
    }

    private const val TAG = "IoApi"

    private val objectTag = ObjectTag("Core", TAG)

    // 注释相应平台的下载软件
    override fun downloadFile(isDebug: Boolean,
                              fileName: String, url: String, headers: Map<String, String>,
                              externalDownloader: Boolean) {
        val ariaDownloader = AriaDownloader(isDebug, url).also {
            it.createDownloadTaskNotification(fileName)
        }
        if (!externalDownloader) {
            try {
                ariaDownloader.start(fileName, headers = headers)
            } catch (e: IllegalArgumentException) {
                Log.e(objectTag, TAG, """ downloadFile: 下载任务失败
                        |下载参数: URL: $url, FileName: $fileName, headers: $headers
                        |ERROR_MESSAGE: $e""".trimIndent())
                ariaDownloader.cancel()
                runBlocking(Dispatchers.Main) {
                    Toast.makeText(context, "下载失败: $fileName", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            MiscellaneousUtils.accessByBrowser(
                    url,
                    context
            )
            ariaDownloader.cancel()
        }
    }

    // 查询软件信息
    override fun getAppVersionNumber(targetChecker: AppConfigGson.AppConfigBean.TargetCheckerBean?) =
            VersioningUtils.getAppVersionNumber(targetChecker)
}
