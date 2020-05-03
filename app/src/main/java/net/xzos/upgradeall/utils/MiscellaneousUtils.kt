package net.xzos.upgradeall.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.jaredrummler.android.shell.CommandResult
import com.jaredrummler.android.shell.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.android_api.DatabaseApi
import net.xzos.upgradeall.android_api.IoApi
import net.xzos.upgradeall.android_api.Log
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.data_manager.CloudConfigGetter
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.server.update.UpdateManager
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import org.json.JSONException
import java.io.StringReader
import java.util.*


object MiscellaneousUtils {

    init {
        renewCloudConfigGetter()
    }

    private var suAvailable: Boolean? = null
        get() = field ?: Shell.SU.available().also { field = it }

    lateinit var cloudConfigGetter: CloudConfigGetter
        private set

    fun initData() {
        initObject()
        PreferencesMap.sync()
    }

    private fun initObject() {
        // 初始化 System API
        DatabaseApi
        Log
        IoApi
        // 初始化数据观察 register
        AppUiDataManager
        UpdateManager
    }

    fun renewCloudConfigGetter() {
        cloudConfigGetter = CloudConfigGetter(PreferencesMap.cloud_rules_hub_url)
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
        val handler = Handler(Looper.getMainLooper())
        handler.post {
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
    Handler(Looper.getMainLooper()).post {
        this.value = this.value
    }
}

/**
 * 拓展 LiveData 设置值操作
 */
fun <T> MutableLiveData<T>.setValueBackstage(value: T) {
    Handler(Looper.getMainLooper()).post {
        this.value = value
    }
}

/**
 * 拓展 ItemCardView 数据队列
 * 使其可用 holder 直接获取数据
 */
fun MutableList<ItemCardView>.getByHolder(holder: CardViewRecyclerViewHolder): ItemCardView =
        this[holder.adapterPosition]

/**
 * 返回 MutableLiveData
 */
fun <T> mutableLiveDataOf(): MutableLiveData<T> = MutableLiveData()
