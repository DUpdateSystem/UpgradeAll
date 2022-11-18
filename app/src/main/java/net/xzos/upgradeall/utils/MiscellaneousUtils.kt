package net.xzos.upgradeall.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.core.androidutils.runUiFun
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.app
import net.xzos.upgradeall.core.utils.log.msg
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


object MiscellaneousUtils {

    private const val TAG = "MiscellaneousUtils"
    private val logObjectTag = ObjectTag(app, TAG)

    private const val HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>"
    private val pattern: Pattern = Pattern.compile(HTML_PATTERN)

    fun CharSequence.hasHTMLTags(): Boolean {
        val matcher: Matcher = pattern.matcher(this)
        return matcher.find()
    }

    fun accessByBrowser(url: String, context: Context, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            packageName?.let { setPackage(it) }
            if (context == MyApplication.context)
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.select_browser))
            )
        } catch (e: Exception) {
            ToastUtil.showText(
                context, R.string.system_browser_error, duration = Toast.LENGTH_LONG
            )
            Log.e(logObjectTag, TAG, "accessByBrowser: ${e.msg()}")
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                ToastUtil.showText(
                    context,
                    R.string.open_browser_failed,
                    duration = Toast.LENGTH_LONG
                )
                Log.e(logObjectTag, TAG, "accessByBrowser: ${e.msg()}")
            }
        }
    }

    fun getCurrentLocale(context: Context): Locale? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.resources.configuration.locales[0]
        else
            @Suppress("DEPRECATION")
            context.resources.configuration.locale

    fun isBackground(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                || appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
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
    runUiFun {
        this.value = value
    }
}

/**
 * 返回 MutableLiveData
 */
fun <T> mutableLiveDataOf(): MutableLiveData<T> = MutableLiveData()

