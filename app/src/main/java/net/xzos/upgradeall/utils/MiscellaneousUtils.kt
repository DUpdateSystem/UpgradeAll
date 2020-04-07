package net.xzos.upgradeall.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.jaredrummler.android.shell.CommandResult
import com.jaredrummler.android.shell.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.android_api.DatabaseApi
import net.xzos.upgradeall.android_api.IoApi
import net.xzos.upgradeall.android_api.Log
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.data.config.AppConfig
import net.xzos.upgradeall.core.data_manager.CloudConfigGetter
import net.xzos.upgradeall.server.update.UpdateManager
import net.xzos.upgradeall.server.update.UpdateServiceReceiver
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import org.json.JSONException
import java.io.StringReader
import java.util.*


object MiscellaneousUtils {

    init {
        renewCloudConfigGetter()
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private var suAvailable: Boolean? = null
        get() = field ?: Shell.SU.available().also { field = it }

    lateinit var cloudConfigGetter: CloudConfigGetter
        private set

    fun initData() {
        // 初始化 System API
        DatabaseApi
        Log
        IoApi
        UpdateManager
        syncSetting()
    }

    private fun renewCloudConfigGetter() {
        cloudConfigGetter = newCloudConfigGetter()
    }

    private fun newCloudConfigGetter(): CloudConfigGetter {
        val prefKey = "cloud_rules_hub_url"
        val defaultCloudRulesHubUrl = AppConfig.default_cloud_rules_hub_url
        val cloudRulesHubUrl = (prefs
                ?: PreferenceManager.getDefaultSharedPreferences(context)
                ).getString(prefKey, defaultCloudRulesHubUrl) ?: defaultCloudRulesHubUrl
        val cloudConfigGetter = CloudConfigGetter(cloudRulesHubUrl)
        if (!cloudConfigGetter.available) {
            resetCloudHubUrl()
        }
        return cloudConfigGetter
    }

    fun resetCloudHubUrl() {
        val prefKey = "cloud_rules_hub_url"
        val defaultCloudRulesHubUrl = AppConfig.default_cloud_rules_hub_url
        prefs.edit().putString(prefKey, defaultCloudRulesHubUrl).apply()
        renewCloudConfigGetter()
        showToast(context, R.string.reset_git_url_configuration, duration = Toast.LENGTH_LONG)
    }

    // 同步设置
    fun syncSetting() {
        // 刷新数据时间
        UpdateServiceReceiver.initAlarms()
        // Git 地址
        renewCloudConfigGetter()
        // 更新服务器地址
        val updateServerUrlKey = "update_server_url"
        val updateServerUrl = prefs.getString(updateServerUrlKey, null)
        if (!AppConfig.setUpdateServerUrl(updateServerUrl)) {
            prefs.edit().putString(updateServerUrlKey, AppConfig.update_server_url).apply()
            showToast(context, R.string.reset_update_server_url_configuration, duration = Toast.LENGTH_LONG)
        }
        val dataExpirationTimeKey = "sync_time"
        val defaultDataExpirationTime = AppConfig.data_expiration_time
        val dataExpirationTime = prefs.getInt(dataExpirationTimeKey, defaultDataExpirationTime)
        if (dataExpirationTime != defaultDataExpirationTime)
            AppConfig.data_expiration_time = dataExpirationTime
    }

    fun accessByBrowser(url: String?, context: Context?) {
        if (url != null && context != null)
            try {
                context.startActivity(
                        Intent.createChooser(
                                Intent(Intent.ACTION_VIEW).apply {
                                    this.data = Uri.parse(url)
                                }, context.getString(R.string.select_browser)).apply {
                            if (context == MyApplication.context)
                                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                )
            } catch (e: ArrayIndexOutOfBoundsException) {
                showToast(context, R.string.miui_error, duration = Toast.LENGTH_LONG)
                Intent(Intent.ACTION_VIEW).apply {
                    this.data = Uri.parse(url)
                }
            }
    }

    fun getCurrentLocale(context: Context): Locale? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                context.resources.configuration.locales[0]
            else
                @Suppress("DEPRECATION")
                context.resources.configuration.locale

    fun parsePropertiesString(s: String): Properties =
            Properties().apply {
                this.load(StringReader(s))
            }

    fun runShellCommand(command: String, su: Boolean = false): CommandResult? =
            if (command.isNotBlank())
                if (su)
                    if (suAvailable!!)
                        Shell.SU.run(command)
                    else {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, R.string.no_root_and_restart_to_use_root,
                                    Toast.LENGTH_LONG).show()
                        }
                        null
                    }
                else Shell.run(command)
            else null

    fun mapOfJsonObject(jsonObjectString: String): Map<*, *> {
        return try {
            Gson().fromJson(jsonObjectString, Map::class.java)
        } catch (e: JSONException) {
            mapOf<Any, Any>()
        }
    }

    fun isBackground(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                || appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    @JvmOverloads
    fun showToast(context: Context?, resId: Int? = null, text: CharSequence? = null, duration: Int = Toast.LENGTH_SHORT) {
        runBlocking(Dispatchers.Main) {
            when {
                text != null -> Toast.makeText(context, text, duration)
                resId != null -> Toast.makeText(context, resId, duration)
                else -> null
            }?.show()
        }
    }
}

/**
 * 拓展 LiveData 监听列表元素添加、删除操作的支持
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}

/**
 * 拓展 ItemCardView 数据队列
 * 使其可用 holder 直接获取数据
 */
fun MutableList<ItemCardView>.getByHolder(holder: CardViewRecyclerViewHolder): ItemCardView =
        this[holder.adapterPosition]
