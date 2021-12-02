package net.xzos.upgradeall.core.androidutils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import me.drakeet.support.toast.ToastCompat

object ToastUtil {
    /**
     * show a toast via a string
     *
     * @param text a string text
     */
    fun showText(context: Context, text: String, duration: Int = Toast.LENGTH_SHORT) {
        runUiFun {
            ToastCompat.makeText(context, text, duration).show()
        }
    }

    /**
     * show a toast via a resource id
     *
     * @param resId a string resource id
     */
    fun showText(context: Context, @StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        runUiFun {
            ToastCompat.makeText(context, context.getText(resId), duration).show()
        }
    }
}