package net.xzos.upgradeall.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.manager.CloudConfigGetter
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.utils.egg.egg
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


object MiscellaneousUtils {

    fun initData() {
        initObject()
        PreferencesMap.sync()
        GlobalScope.launch { renewData() }
        egg()
    }

    private fun initObject() {
    }

    private suspend fun renewData() {
        if (PreferencesMap.auto_update_hub_config) {
            CloudConfigGetter.renewAllHubConfigFromCloud()
        }
        if (PreferencesMap.auto_update_app_config) {
            CloudConfigGetter.renewAllAppConfigFromCloud()
        }
    }

    private const val HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>"
    private val pattern: Pattern = Pattern.compile(HTML_PATTERN)

    fun CharSequence.hasHTMLTags(): Boolean {
        val matcher: Matcher = pattern.matcher(this)
        return matcher.find()
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
        runUiFun {
            ToastUtil.makeText(resId, duration)
        }
    }

    fun showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        runUiFun {
            ToastUtil.makeText(text.toString(), duration)
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (ignore: PackageManager.NameNotFoundException) {
            null
        }
    }
}

/**
 * 拓展 LiveData 监听列表元素添加、删除操作的支持
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    runUiFun {
        this.value = this.value
    }
}

/**
 * 拓展 LiveData 设置值操作
 */
fun <T> MutableLiveData<T>.setValueBackground(value: T) {
    if (value == this.value) return
    runUiFun {
        this.value = value
    }
}

/**
 * 返回 MutableLiveData
 */
fun <T> mutableLiveDataOf(): MutableLiveData<T> = MutableLiveData()

fun runUiFun(f: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        f()
    } else {
        Handler(Looper.getMainLooper()).post {
            f()
        }
    }
}
