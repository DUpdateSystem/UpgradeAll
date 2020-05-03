package net.xzos.upgradeall.data

import android.widget.Toast
import androidx.preference.PreferenceManager
import com.arialyy.aria.core.Aria
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data_manager.CloudConfigGetter
import net.xzos.upgradeall.core.network_api.GrpcApi
import net.xzos.upgradeall.server.update.UpdateServiceReceiver
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.MiscellaneousUtils.showToast

object PreferencesMap {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // 更新首选项
    private const val updateServerUrlKey = "update_server_url"
    private const val cloudRulesHubUrlKey = "cloud_rules_hub_url"
    val cloud_rules_hub_url: String
        get() = prefs.getString(cloudRulesHubUrlKey, context.getString(R.string.default_cloud_rules_hub_url))!!
    var cloud_rules_hub_url_temp: String = cloud_rules_hub_url
    val update_server_url: String
        get() = prefs.getString(updateServerUrlKey, AppConfig.update_server_url)!!
    val background_sync_time
        get() = prefs.getInt("background_sync_time", 18)

    // 安装首选项
    val auto_install
        get() = prefs.getBoolean("auto_install", true)
    val auto_delete_file
        get() = prefs.getBoolean("auto_delete_file", false)

    // 下载首选项
    private const val downloadPathKey = "user_download_path"
    var user_download_path: String
        get() = prefs.getString(downloadPathKey, null) ?: context.getString(R.string.null_english)
        set(value) {
            prefs.edit().putString(downloadPathKey, value).apply()
            auto_dump_download_file = true
        }
    private const val autoDumpDownloadFileKey = "auto_dump_download_file"
    var auto_dump_download_file: Boolean
        get() = prefs.getBoolean(autoDumpDownloadFileKey, false)
        set(value) = prefs.edit().putBoolean(autoDumpDownloadFileKey, value).apply()
    private val download_thread_num
        get() = prefs.getInt("download_thread_num", 6)
    private val download_max_task_num
        get() = prefs.getInt("download_max_task_num", 8)

    // 检查设置
    // 设置 Android 平台设置
    // 设置内核设置
    fun sync() {
        checkSetting()
        checkUpdateSettingAndSync()
        syncAndroidConfig()
        syncCoreConfig()
    }

    private fun syncAndroidConfig() {
        UpdateServiceReceiver.setAlarms(background_sync_time)
        Aria.get(context).downloadConfig.let {
            it.maxTaskNum = download_max_task_num
            it.threadNum = download_thread_num
        }
    }

    // 同步 Core 模块的配置
    private fun syncCoreConfig() {}

    private fun checkSetting() {
        if (FileUtil.DOWNLOAD_DOCUMENT_FILE?.canWrite() != true)
            auto_dump_download_file = false
    }

    // 检查设置数据
    private fun checkUpdateSettingAndSync() {
        if (GrpcApi.checkUpdateServerUrl(update_server_url)) {
            AppConfig.update_server_url = update_server_url
        } else {
            prefs.edit().putString(updateServerUrlKey, AppConfig.update_server_url).apply()
            showToast(context, R.string.reset_update_server_url_configuration, duration = Toast.LENGTH_LONG)
        }
        val cloudConfigGetter = CloudConfigGetter(cloud_rules_hub_url)
        if (cloudConfigGetter.available) {
            cloud_rules_hub_url_temp = cloud_rules_hub_url
            MiscellaneousUtils.renewCloudConfigGetter()
        } else {
            prefs.edit().putString(cloudRulesHubUrlKey, cloud_rules_hub_url_temp).apply()
            showToast(context, R.string.reset_git_url_configuration, duration = Toast.LENGTH_LONG)
            MiscellaneousUtils.renewCloudConfigGetter()
        }
    }
}
