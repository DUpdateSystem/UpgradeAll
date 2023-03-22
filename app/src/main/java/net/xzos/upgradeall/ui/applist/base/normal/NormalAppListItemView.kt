package net.xzos.upgradeall.ui.applist.base.normal

import android.content.Context
import androidx.databinding.ObservableField
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.AppStatus
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.BaseAppListItemView

open class NormalAppListItemView(app: App) : BaseAppListItemView(app) {

    val statusIcon: ObservableField<Int> = ObservableField()
    val ivStatusVisibility: ObservableField<Boolean> = ObservableField()
    val pbStatusVisibility: ObservableField<Boolean> = ObservableField()

    open fun renew(context: Context) {
        renewData(context)
        if (app.isRenewing()) {
            ivStatusVisibility.set(false)
            pbStatusVisibility.set(true)
        } else {
            setAppStatusIcon(app.getReleaseStatus())
        }
    }

    protected fun setAppStatusIcon(releaseStatus: AppStatus) {
        ivStatusVisibility.set(true)
        pbStatusVisibility.set(false)
        statusIcon.set(
            when (releaseStatus) {
                AppStatus.APP_LATEST -> R.drawable.ic_check_mark_circle
                AppStatus.APP_OUTDATED -> R.drawable.ic_check_needupdate
                AppStatus.NETWORK_ERROR -> R.drawable.ic_del_or_error
                AppStatus.APP_NO_LOCAL -> R.drawable.ic_local_error
                else -> R.drawable.ic_check_mark_circle
            }
        )
    }
}