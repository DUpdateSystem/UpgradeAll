package net.xzos.upgradeall.ui.hubmanager

import android.content.Context
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.hubmanager.setting.HubSettingDialog

class HubManagerListItemHandler : RecyclerViewHandler() {
    fun onCardViewClick(context: Context, hubUuid: String) {
        HubSettingDialog(hubUuid).show(context)
    }
}