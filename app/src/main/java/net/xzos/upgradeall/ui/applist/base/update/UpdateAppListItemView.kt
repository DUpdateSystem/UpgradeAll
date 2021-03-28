package net.xzos.upgradeall.ui.applist.base.update

import android.content.Context
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.core.utils.android_app.getPackageId
import net.xzos.upgradeall.ui.applist.base.BaseAppListItemView

class UpdateAppListItemView(app: App) : BaseAppListItemView(app) {
    fun renew(context: Context) {
        renewAppIcon(app.appId.getPackageId()?.second, context)
    }
}