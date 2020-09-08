package net.xzos.upgradeall.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import net.xzos.upgradeall.R
import net.xzos.upgradeall.android_api.DatabaseApi
import net.xzos.upgradeall.android_api.IoApi
import net.xzos.upgradeall.android_api.Log
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.data.AppUiDataManager
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import org.json.JSONException
import java.io.StringReader
import java.util.*


object MiscellaneousUtils {

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
            } catch (e: Exception) {
                showToast(R.string.system_browser_error, duration = Toast.LENGTH_LONG)
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

    fun mapOfJsonObject(jsonObjectString: String): Map<*, *> {
        return try {
            Gson().fromJson(jsonObjectString, Map::class.java)
        } catch (e: JSONException) {
            mapOf<Any, Any>()
        }
    }

    fun requestPermission(activity: Activity, permission: String, PERMISSIONS_REQUEST_CONTACTS: Int, tipResId: Int): Boolean {
        var havePermission = false
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                showToast(tipResId)
            }
            ActivityCompat.requestPermissions(activity,
                    arrayOf(permission),
                    PERMISSIONS_REQUEST_CONTACTS)
        } else
            havePermission = true
        return havePermission
    }

    fun isBackground(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                || appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }

    fun showToast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Handler(Looper.getMainLooper()).post {
            ToastUtil.makeText(resId, duration)
        }
    }

    fun showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        Handler(Looper.getMainLooper()).post {
            ToastUtil.makeText(text.toString(), duration)
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
fun <T> MutableLiveData<T>.setValueBackground(value: T) {
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
