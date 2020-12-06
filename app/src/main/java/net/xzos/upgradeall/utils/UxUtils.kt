package net.xzos.upgradeall.utils

import android.content.res.Resources

object UxUtils {
    fun getStatusBarHeight(resources: Resources): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }
}