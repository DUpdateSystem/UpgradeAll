package net.xzos.upgradeall.ui.applist.base.normal

import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.ui.applist.base.BaseAppListItemView

class NormalAppListItemView(val app: App) : BaseAppListItemView(app) {

    suspend fun getStatusIcon(): Int {
        return when (app.getReleaseStatusWaitRenew()) {
            Updater.APP_LATEST -> R.drawable.ic_check_mark_circle
            Updater.APP_OUTDATED -> R.drawable.ic_check_needupdate
            Updater.NETWORK_ERROR -> R.drawable.ic_del_or_error
            Updater.APP_NO_LOCAL -> R.drawable.ic_local_error
            else -> R.drawable.ic_check_mark_circle
        }
    }
}