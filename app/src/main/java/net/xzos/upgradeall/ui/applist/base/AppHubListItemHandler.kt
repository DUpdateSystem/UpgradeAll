package net.xzos.upgradeall.ui.applist.base

import android.view.View
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.detail.AppDetailActivity

abstract class AppHubListItemHandler : RecyclerViewHandler() {
    fun onClickApp(view: View, app: App) {
        AppDetailActivity.startActivity(view.context, app)
    }
}