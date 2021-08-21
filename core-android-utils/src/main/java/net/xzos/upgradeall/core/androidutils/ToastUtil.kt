package net.xzos.upgradeall.core.androidutils

import android.widget.Toast
import androidx.annotation.StringRes
import me.drakeet.support.toast.ToastCompat

object ToastUtil {
    /**
     * make a toast via a string
     *
     * @param text a string text
     */
    fun makeText(text: String, duration: Int = Toast.LENGTH_SHORT) {
        ToastCompat.makeText(androidContext, text, duration).show()
    }

    /**
     * make a toast via a resource id
     *
     * @param resId a string resource id
     */
    fun makeText(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        ToastCompat.makeText(androidContext, androidContext.getText(resId), duration).show()
    }
}