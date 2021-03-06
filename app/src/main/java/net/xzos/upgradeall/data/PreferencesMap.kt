package net.xzos.upgradeall.data

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data.config.AppValue
import net.xzos.upgradeall.server.downloader.Downloader
import net.xzos.upgradeall.server.update.UpdateServiceBroadcastReceiver
import net.xzos.upgradeall.utils.MiscellaneousUtils.showToast
import net.xzos.upgradeall.utils.file.FileUtil
import net.xzos.upgradeall.utils.install.ApkShizukuInstaller
import java.util.*

object PreferencesMap {
    private lateinit var context: Context
    fun setContext(context: Context) {
        this.context = context
    }

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    // 更新首选项
    private const val UPDATE_SERVER_URL_KEY = "update_server_url"
    const val CUSTOM_CLOUD_RULES_HUB_URL_KEY = "custom_cloud_rules_hub_url"
    private const val CLOUD_RULES_HUB_URL_KEY = "cloud_rules_hub_url"
    val custom_cloud_rules_hub_url: Boolean
        get() {
            val customCloudRulesHubUrl = prefs.getString(CUSTOM_CLOUD_RULES_HUB_URL_KEY, AppValue.default_cloud_rules_hub_url)!!
            return customCloudRulesHubUrl != AppValue.default_cloud_rules_hub_url
        }

    var cloud_rules_hub_url: String
        get() = prefs.getString(CLOUD_RULES_HUB_URL_KEY, AppValue.default_cloud_rules_hub_url)!!
        set(value) = prefs.edit().putString(CLOUD_RULES_HUB_URL_KEY, value).apply()
    private var update_server_url: String
        get() = prefs.getString(UPDATE_SERVER_URL_KEY, AppConfig.update_server_url)!!
        set(value) = prefs.edit().putString(UPDATE_SERVER_URL_KEY, value).apply()

    val auto_update_app_config: Boolean
        get() = prefs.getBoolean("auto_update_app_config", true)
    val auto_update_hub_config: Boolean
        get() = prefs.getBoolean("auto_update_hub_config", true)
    private val background_sync_time
        get() = prefs.getInt("background_sync_time", 18)

    // 安装首选项
    val install_apk_api
        get() = prefs.getString("install_apk_api", "System")
    val auto_install
        get() = prefs.getBoolean("auto_install", true)
    val auto_delete_file
        get() = prefs.getBoolean("auto_delete_file", false)

    // 下载首选项
    private const val DOWNLOAD_PATH_KEY = "user_download_path"
    var user_download_path: String
        get() = prefs.getString(DOWNLOAD_PATH_KEY, null)
                ?: context.getString(R.string.please_grant_storage_perm)
        set(value) {
            prefs.edit().putString(DOWNLOAD_PATH_KEY, value).apply()
            auto_dump_download_file = true
        }
    private const val AUTO_DUMP_DOWNLOAD_FILE_KEY = "auto_dump_download_file"
    var auto_dump_download_file: Boolean
        get() = prefs.getBoolean(AUTO_DUMP_DOWNLOAD_FILE_KEY, false)
        set(value) = prefs.edit().putBoolean(AUTO_DUMP_DOWNLOAD_FILE_KEY, value).apply()
    internal const val DOWNLOAD_THREAD_NUM_KEY = "download_thread_num"
    var download_thread_num: Int
        get() = prefs.getInt(DOWNLOAD_THREAD_NUM_KEY, 6)
        set(value) = prefs.edit().putInt(DOWNLOAD_THREAD_NUM_KEY, value).apply()
    internal const val DOWNLOAD_MAX_TASK_NUM_KEY = "download_max_task_num"
    var download_max_task_num: Int
        get() = prefs.getInt(DOWNLOAD_MAX_TASK_NUM_KEY, 8)
        set(value) = prefs.edit().putInt(DOWNLOAD_MAX_TASK_NUM_KEY, value).apply()

    internal const val DOWNLOAD_AUTO_RETRY_MAX_ATTEMPTS_KEY = "download_auto_retry_max_attempts"
    var download_auto_retry_max_attempts: Int
        get() = prefs.getInt(DOWNLOAD_AUTO_RETRY_MAX_ATTEMPTS_KEY, 3)
        set(value) = prefs.edit().putInt(DOWNLOAD_AUTO_RETRY_MAX_ATTEMPTS_KEY, value).apply()

    val enforce_use_external_downloader: Boolean
        get() = prefs.getBoolean("enforce_use_external_downloader", false)

    // WebDAV
    val webdav_url
        get() = prefs.getString("webdav_url", null)
    val webdav_path
        get() = prefs.getString("webdav_path", null)
    val webdav_username
        get() = prefs.getString("webdav_username", null)
    val webdav_password
        get() = prefs.getString("webdav_password", null)

    // Language
    private const val LOCALE_CUSTOM_KEY = "locale_custom"
    private val locale_custom
        get() = prefs.getBoolean(LOCALE_CUSTOM_KEY, false)

    private const val LANGUAGE_LOCALE_CODE_KEY = "language_locale_code"
    val custom_language_locale: Locale?
        get() = if (locale_custom) {
            when (prefs.getString(LANGUAGE_LOCALE_CODE_KEY, null)) {
                "zh_CN" -> Locale("zh", "CN")
                "en_US" -> Locale("en", "US")
                else -> null
            }
        } else null

    fun initByActivity(activity: Activity) {
        if (install_apk_api == "Shizuku") {
            ApkShizukuInstaller.requestShizukuPermission(activity, 0)
        }
    }

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
        UpdateServiceBroadcastReceiver.setAlarms(background_sync_time)
        Downloader.renewFetch(context)
    }

    // 同步 Core 模块的配置
    private fun syncCoreConfig() {
        AppConfig.app_cloud_rules_hub_url = if (custom_cloud_rules_hub_url)
            cloud_rules_hub_url
        else null
    }

    private fun checkSetting() {
        if (download_thread_num <= 0)
            download_thread_num = 1
        if (download_max_task_num <= 0)
            download_max_task_num = 1
        if (download_auto_retry_max_attempts <= 0)
            download_auto_retry_max_attempts = 1
        if (FileUtil.DOWNLOAD_DOCUMENT_FILE?.canWrite() != true)
            auto_dump_download_file = false
    }

    // 检查设置数据
    private fun checkUpdateSettingAndSync() {
        AppConfig.update_server_url = update_server_url  // 可能需要重启客户端才能同步
        if (AppConfig.update_server_url != update_server_url) {
            update_server_url = AppConfig.update_server_url
            showToast(R.string.reset_update_server_url_configuration, duration = Toast.LENGTH_LONG)
        }
    }
}
