package net.xzos.upgradeall.utils

import android.widget.Toast
import androidx.annotation.StringRes
import me.drakeet.support.toast.ToastCompat
import net.xzos.upgradeall.application.MyApplication

object ToastUtil {
    /**
     * make a toast via a string
     *
     * @param text a string text
     */
    fun makeText(text: String, duration: Int = Toast.LENGTH_SHORT) {
        ToastCompat.makeText(MyApplication.context, text, duration).show()
    }

    /**
     * make a toast via a resource id
     *
     * @param resId a string resource id
     */
    fun makeText(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        ToastCompat.makeText(MyApplication.context, MyApplication.context.getText(resId), duration).show()
    }
}