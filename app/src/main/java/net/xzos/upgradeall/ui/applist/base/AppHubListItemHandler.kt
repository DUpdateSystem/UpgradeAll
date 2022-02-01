package net.xzos.upgradeall.ui.applist.base

import android.view.View
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.detail.AppDetailActivity

abstract class AppHubListItemHandler : RecyclerViewHandler() {
    fun onClickApp(app: App, view: View) {
        AppDetailActivity.startActivity(view.context, app)
    }

    abstract fun showPopup(app: App, v: View): Boolean
}