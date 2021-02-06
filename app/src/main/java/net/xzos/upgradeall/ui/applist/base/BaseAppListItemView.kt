package net.xzos.upgradeall.ui.applist.base

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.getPackageId
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.UxUtils

abstract class BaseAppListItemView(private val app: App) : ListItemView(app.name) {

    override val name get() = app.name
    override val nameFirst: String
        get() {
            return if (appIcon == null)
                super.nameFirst
            else ""
        }

    val iconBackgroundTint get() = if (appIcon == null) ColorStateList.valueOf(UxUtils.getRandomColor()) else null

    private val appIcon: Drawable? by lazy {
        app.appId.getPackageId()?.second?.run {
            MiscellaneousUtils.getAppIcon(context, this)?.run {
                return@lazy this
            }
        }
        return@lazy null
    }
    val icon: Drawable by lazy {
        return@lazy appIcon ?: ContextCompat.getDrawable(context, R.drawable.bg_circle)!!
    }

    val version: String
        get() {
            val latestVersionNumber = app.getLatestVersionNumber()
            val installedVersionNumber = app.installedVersionNumber
            return when {
                installedVersionNumber == null -> latestVersionNumber ?: ""
                latestVersionNumber == installedVersionNumber -> installedVersionNumber
                else -> "$installedVersionNumber > $latestVersionNumber"
            }
        }

    override fun equals(other: Any?): Boolean {
        return other is BaseAppListItemView
                && other.app == app
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + app.hashCode()
        return result
    }

    companion object {
        private val context get() = MyApplication.context
    }
}