package net.xzos.upgradeall.ui.applist.base.applications

import android.content.Context
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.normal.NormalAppListItemView

class ApplicationsAppHubListItemView(app: App) : NormalAppListItemView(app) {
    override fun renew(context: Context) {
        renewData(context)
        val releaseStatus = app.releaseStatus
        if (app.isRenewing) {
            if (releaseStatus != AppStatus.NETWORK_ERROR) {
                setAppStatusIcon(releaseStatus)
            } else {
                ivStatusVisibility.set(false)
                pbStatusVisibility.set(true)
            }
        } else {
            setAppStatusIcon(releaseStatus)
        }
    }
}