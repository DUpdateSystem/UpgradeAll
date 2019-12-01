package net.xzos.upgradeAll.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import net.xzos.upgradeAll.application.MyApplication
import java.io.StringReader
import java.util.*

object MiscellaneousUtils {
    fun accessByBrowser(url: String?, context: Context?) {
        if (url != null && context != null) {
            context.startActivity(
                    Intent.createChooser(
                            Intent(Intent.ACTION_VIEW).apply {
                                this.data = Uri.parse(url)
                            }, "请选择浏览器以打开网页").apply {
                        if (context == MyApplication.context)
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
            )
        }
    }

    fun getCurrentLocale(context: Context): Locale? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    fun parsePropertiesString(s: String): Properties {
        return Properties().apply {
            this.load(StringReader(s))
        }
    }
}