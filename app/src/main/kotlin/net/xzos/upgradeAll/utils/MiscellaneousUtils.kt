package net.xzos.upgradeAll.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

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
}