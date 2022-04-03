package net.xzos.upgradeall.ui.hubmanager

import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.base.list.HubListActivity
import net.xzos.upgradeall.ui.hubmanager.setting.HubSettingDialog


class HubManagerActivity : HubListActivity<HubManagerListItemView, HubManagerListItemView, HubManagerListViewHolder>() {
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(R.string.global_setting)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.title == getString(R.string.global_setting)) {
            HubSettingDialog(null).show(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override val viewModel by viewModels<HubManagerViewModel>()
    override val adapter by lazy { HubManagerAdapter(HubManagerListItemHandler()) }
}