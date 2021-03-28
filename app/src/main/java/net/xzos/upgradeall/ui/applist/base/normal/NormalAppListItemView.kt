package net.xzos.upgradeall.ui.applist.base.normal

import android.content.Context
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.module.app.Updater
import net.xzos.upgradeall.core.utils.android_app.getPackageId
import net.xzos.upgradeall.ui.applist.base.BaseAppListItemView

class NormalAppListItemView(app: App) : BaseAppListItemView(app) {

    val statusIcon: ObservableField<Int> = ObservableField()
    val ivStatusVisibility: ObservableField<Boolean> = ObservableField()
    val pbStatusVisibility: ObservableField<Boolean> = ObservableField()

    fun renew(context: Context) {
        renewAppIcon(app.appId.getPackageId()?.second, context)
        if (app.isRenewing()) {
            ivStatusVisibility.set(false)
            pbStatusVisibility.set(true)
        } else {
            ivStatusVisibility.set(true)
            pbStatusVisibility.set(false)
            statusIcon.set(when (app.getReleaseStatus()) {
                Updater.APP_LATEST -> R.drawable.ic_check_mark_circle
                Updater.APP_OUTDATED -> R.drawable.ic_check_needupdate
                Updater.NETWORK_ERROR -> R.drawable.ic_del_or_error
                Updater.APP_NO_LOCAL -> R.drawable.ic_local_error
                else -> R.drawable.ic_check_mark_circle
            })
        }
    }
}