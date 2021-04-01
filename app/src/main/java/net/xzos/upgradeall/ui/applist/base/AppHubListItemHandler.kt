package net.xzos.upgradeall.ui.applist.base

import android.view.View
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.manager.AppManager
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.detail.AppDetailActivity
import net.xzos.upgradeall.ui.detail.setting.AppSettingActivity

abstract class AppHubListItemHandler : RecyclerViewHandler() {
    fun onClickApp(app: App, view: View) {
        AppDetailActivity.startActivity(view.context, app)
    }

    fun showPopup(app: App, v: View): Boolean {
        PopupMenu(v.context, v).apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_app -> {
                        AppSettingActivity.startActivity(v.context, app)
                        true
                    }
                    R.id.del_app -> {
                        runBlocking { AppManager.removeApp(app) }
                        true
                    }
                    else -> false
                }
            }
            inflate(R.menu.menu_app_item)
        }.show()
        return true
    }
}