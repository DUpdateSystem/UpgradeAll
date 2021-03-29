package net.xzos.upgradeall.ui.base.list

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.UxUtils

interface BaseAppIconItem {
    val appName: ObservableField<String>
    val nameFirst: ObservableField<String>
    val appIcon: ObservableField<Drawable>
    val iconBackgroundTint: ObservableField<ColorStateList?>

    fun renewAppIcon(packageId: String?, context: Context) {
        var appIcon: Drawable? = null
        packageId?.run {
            appIcon = MiscellaneousUtils.getAppIcon(context, packageId)
        }
        this.appIcon.set(appIcon ?: ContextCompat.getDrawable(context, R.drawable.bg_circle)!!)
        val name = appName.get()
        nameFirst.set(if (appIcon == null && name != null) UxUtils.getFirstChar(name, true) else "")
        iconBackgroundTint.set(if (appIcon == null) ColorStateList.valueOf(UxUtils.getRandomColor()) else null)
    }
}