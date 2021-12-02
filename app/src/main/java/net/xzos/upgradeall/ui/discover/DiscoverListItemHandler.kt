package net.xzos.upgradeall.ui.discover

import android.view.View
import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler

class DiscoverListItemHandler(
        private val viewModel: DiscoverViewModel,
        private val supportFragmentManager: FragmentManager,
) : RecyclerViewHandler() {
    fun onClickDiscover(view: View, uuid: String) {
        try {
            ConfigDownloadDialog(uuid) {
                viewModel.loadData()
            }.show(supportFragmentManager)
        } catch (e: IllegalStateException) {
            ToastUtil.showText(view.context, e.message.toString())
            viewModel.loadData()
        }
    }
}