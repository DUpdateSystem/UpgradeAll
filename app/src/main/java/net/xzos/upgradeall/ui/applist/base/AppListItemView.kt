package net.xzos.upgradeall.ui.applist.base

import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.base.list.ListItemView

class AppListItemView(val app: App) : ListItemView(app.name) {

    override val name get() = app.name
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
        return other is AppListItemView
                && other.app == app
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + app.hashCode()
        return result
    }
}