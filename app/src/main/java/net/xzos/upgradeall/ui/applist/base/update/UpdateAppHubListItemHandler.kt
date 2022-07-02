package net.xzos.upgradeall.ui.applist.base.update

import android.view.View
import androidx.appcompat.widget.PopupMenu
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.module.app.App
import net.xzos.upgradeall.ui.applist.base.AppHubListItemHandler
import net.xzos.upgradeall.wrapper.core.isIgnored
import net.xzos.upgradeall.wrapper.core.switchIgnoreStatus
import net.xzos.upgradeall.wrapper.core.upgrade

class UpdateAppHubListItemHandler : AppHubListItemHandler() {
    fun clickDownload(app: App, view: View) {
        GlobalScope.launch {
            app.upgrade(view.context)
        }
    }

    override fun showPopup(app: App, v: View): Boolean {
        PopupMenu(v.context, v).apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.ignore_update -> {
                        app.versionList.firstOrNull()?.switchIgnoreStatus(app)
                        true
                    }
                    else -> false
                }
            }
            inflate(R.menu.menu_app_item_update)
        }.show()
        return true
    }
}