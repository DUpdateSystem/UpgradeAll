package net.xzos.upgradeall.data

import android.app.Activity
import android.content.Context
import androidx.preference.PreferenceManager
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.CoreConfig
import net.xzos.upgradeall.core.data.DEF_UPDATE_SERVER_URL
import net.xzos.upgradeall.core.data.WebDavConfig
import net.xzos.upgradeall.core.initCore
import net.xzos.upgradeall.core.installer.ApkShizukuInstaller
import net.xzos.upgradeall.server.downloader.DownloadNotification
import net.xzos.upgradeall.server.update.UpdateServiceBroadcastReceiver
import net.xzos.upgradeall.ui.home.MainActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.file.FileUtil
import java.util.*


object PreferencesMap {
    private lateinit var getContextFun: () -> Context
    private val context: Context get() = getContextFun()
    fun setContext(getContextFun: () -> Context) {
        this.getContextFun = getContextFun
    }

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    // 更新首选项
    private const val UPDATE_SERVER_URL_KEY = "update_server_url"
    const val CUSTOM_CLOUD_RULES_HUB_URL_KEY = "custom_cloud_rules_hub_url"
    private const val CLOUD_RULES_HUB_URL_KEY = "cloud_rules_hub_url"
    val custom_cloud_rules_hub_url: Boolean
        get() {
            val customCloudRulesHubUrl = prefs.getString(CUSTOM_CLOUD_RULES_HUB_URL_KEY, "FromUpdateServer")!!
            return customCloudRulesHubUrl.toLowerCase(Locale.ENGLISH) == "Custom"
        }

    var cloud_rules_hub_url: String?
        get() = prefs.getString(CLOUD_RULES_HUB_URL_KEY, null)
        set(value) = prefs.edit().putString(CLOUD_RULES_HUB_URL_KEY, value).apply()
    private var update_server_url: String
        get() = prefs.getString(UPDATE_SERVER_URL_KEY, DEF_UPDATE_SERVER_URL)!!
        set(value) = prefs.edit().putString(UPDATE_SERVER_URL_KEY, value).apply()

    val auto_update_app_config: Boolean
        get() = prefs.getBoolean("auto_update_app_config", true)
    val auto_update_hub_config: Boolean
        get() = prefs.getBoolean("auto_update_hub_config", true)
    private val background_sync_time
        get() = prefs.getInt("background_sync_time", 18)

    val applications_ignore_system_app: Boolean
        get() = prefs.getBoolean("applications_ignore_system_app", true)

    // 安装首选项
    private val install_apk_api: String
        get() = prefs.getString("install_apk_api", "System")!!
    val auto_install: Boolean
        get() = prefs.getBoolean("auto_install", true)
    val auto_delete_file: Boolean
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
        get() = prefs.getBoolean("enforce_use_external_downloader", true)

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

    //UI
    private const val ENABLE_SIMPLE_BOTTOM_MAIN = "enable_simple_bottom_main"
    val enable_simple_bottom_main
        get() = prefs.getBoolean(ENABLE_SIMPLE_BOTTOM_MAIN, false)

    private const val HOME_BOTTOM_QUEUE_CUSTOM_KEY = "HOME_BOTTOM_LIST"
    private var home_bottom_queue_string: String?
        get() = prefs.getString(HOME_BOTTOM_QUEUE_CUSTOM_KEY, null)
        set(value) = prefs.edit().putString(HOME_BOTTOM_QUEUE_CUSTOM_KEY, value).apply()

    val home_bottom_queue: List<String>
        get() = home_bottom_queue_string?.split(" ")?.toMutableList() ?: defHomeBottomIdList
    var home_bottom_map: Map<String, Boolean>
        get() {
            val map = home_bottom_queue.map { it to true }.toMap().toMutableMap()
            defHomeBottomIdList.forEach {
                if (!map.containsKey(it)) {
                    map[it] = false
                }
            }
            return map
        }
        set(value) {
            home_bottom_queue_string = value.filterValues { it }.keys.joinToString(" ")
        }

    private val defHomeBottomIdList = listOf(
            MainActivity.HOME_MODULE_DISCOVERY,
            MainActivity.HOME_MODULE_HUB_MANAGER,
            MainActivity.HOME_MODULE_FILE_MANAGER,
            MainActivity.HOME_MODULE_APPS_LIST,
            MainActivity.HOME_MODULE_MAGISK_LIST,
    )

    fun initByActivity(activity: Activity) {
        if (install_apk_api == "Shizuku") {
            ApkShizukuInstaller.initByActivity(activity, 1)
        }
    }

    fun sync() {
        checkSetting()
        syncCoreConfig()
        checkUpdateSettingAndSync()
        syncAndroidConfig()
    }

    // 设置 Android 平台设置
    private fun syncAndroidConfig() {
        UpdateServiceBroadcastReceiver.setAlarms(background_sync_time)
    }

    // 同步 Core 模块的配置
    private fun syncCoreConfig() {
        val coreConfig = CoreConfig(
                androidContext = MyApplication.context,
                data_expiration_time = 10,
                update_server_url = update_server_url,
                cloud_rules_hub_url = cloud_rules_hub_url,
                download_document_file = FileUtil.DOWNLOAD_DOCUMENT_FILE,
                download_max_task_num = download_max_task_num,
                download_thread_num = download_thread_num,
                download_auto_retry_max_attempts = download_auto_retry_max_attempts,
                install_apk_api = install_apk_api,
                applications_ignore_system_app = applications_ignore_system_app,
        )
        initCore(coreConfig,
                WebDavConfig(webdav_url, webdav_path, webdav_username, webdav_password),
                DownloadNotification.downloadServiceNotificationMaker
        )
    }

    // 检查设置
    private fun checkSetting() {
        if (download_thread_num <= 0)
            download_thread_num = 1
        if (download_max_task_num <= 0)
            download_max_task_num = 1
        if (download_auto_retry_max_attempts <= 0)
            download_auto_retry_max_attempts = 1
        if (auto_dump_download_file && FileUtil.getUserDownloadDocumentFile()?.canWrite() != true) {
            auto_dump_download_file = false
            MiscellaneousUtils.showToast(R.string.download_dir_check_filed)
        }
    }

    // 检查设置数据
    private fun checkUpdateSettingAndSync() {}
}