package net.xzos.upgradeall.ui.applist.base

import android.graphics.drawable.Drawable
import net.xzos.upgradeall.application.MyApplication.Companion.context
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.getPackageId
import net.xzos.upgradeall.ui.base.list.ListItemView

abstract class BaseAppListItemView(private val app: App) : ListItemView(app.name) {

    override val name get() = app.name

    val icon: Drawable? by lazy {
        context.packageManager.getApplicationIcon(app.appId.getPackageId()?.second
                ?: return@lazy null)
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
}