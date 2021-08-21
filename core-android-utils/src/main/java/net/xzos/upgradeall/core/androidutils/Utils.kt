package net.xzos.upgradeall.core.androidutils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import net.xzos.upgradeall.core.utils.log.Log
import net.xzos.upgradeall.core.utils.log.ObjectTag
import net.xzos.upgradeall.core.utils.log.ObjectTag.Companion.core
import java.util.*

val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    androidContext.resources.configuration.locales.get(0)
} else {
    @Suppress("DEPRECATION")
    androidContext.resources.configuration.locale
}

// 添加粘贴板
fun clipStringToClipboard(s: CharSequence, context: Context = androidContext, @StringRes toastResId: Int? = null) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val mClipData = ClipData.newPlainText("Label", s)
    cm.setPrimaryClip(mClipData)
    toastResId?.run { ToastUtil.makeText(this, Toast.LENGTH_SHORT) }
}

fun requestPermission(
    activity: Activity,
    permission: String,
    PERMISSIONS_REQUEST_CONTACTS: Int,
    tip: String
): Boolean {
    val tag = "RequestPermission"
    val logObjectTag = ObjectTag(core, tag)
    var havePermission = false
    if (ContextCompat.checkSelfPermission(
            activity,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            Log.i(logObjectTag, tag, tip)
        }
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            PERMISSIONS_REQUEST_CONTACTS
        )
    } else
        havePermission = true
    return havePermission
}

fun requestPermission(activity: Activity, permission: String, PERMISSIONS_REQUEST_CONTACTS: Int, @StringRes tipResId: Int): Boolean {
    var havePermission = false
    if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            ToastUtil.makeText(tipResId, Toast.LENGTH_SHORT)
        }
        ActivityCompat.requestPermissions(activity,
            arrayOf(permission),
            PERMISSIONS_REQUEST_CONTACTS)
    } else
        havePermission = true
    return havePermission
}

