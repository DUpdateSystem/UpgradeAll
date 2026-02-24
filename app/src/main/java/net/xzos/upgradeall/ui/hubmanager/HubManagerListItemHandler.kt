package net.xzos.upgradeall.ui.hubmanager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.hubmanager.setting.HubSettingDialog

class HubManagerListItemHandler : RecyclerViewHandler() {
    fun onCardViewClick(context: Context, hubUuid: String) {
        val fm = (context as AppCompatActivity).supportFragmentManager
        HubSettingDialog.newInstance(hubUuid).show(fm, HubSettingDialog.TAG)
    }
}
