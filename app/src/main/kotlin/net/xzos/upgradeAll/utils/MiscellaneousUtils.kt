package net.xzos.upgradeAll.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.StringReader
import java.util.*

object MiscellaneousUtils {
    fun accessByBrowser(url: String?, context: Context?) {
        if (url != null && context != null) {
            Intent(Intent.ACTION_VIEW).apply {
                this.data = Uri.parse(url)
            }.let {
                context.startActivity(
                        Intent.createChooser(it, "请选择浏览器以打开网页")
                )
            }
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