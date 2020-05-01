package net.xzos.upgradeall.ui.viewmodels.callback

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.xzos.upgradeall.core.server_manager.module.BaseApp
import net.xzos.upgradeall.data.gson.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.ui.viewmodels.adapters.AppItemAdapter
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter


class AppItemTouchHelperCallback(
        private val mAdapter: AppItemAdapter,
        private val list: MutableList<BaseApp>
) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }

    override fun getMovementFlags(recyclerView: RecyclerView,
                                  viewHolder: ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder,
                        target: ViewHolder): Boolean {
        if (AppTabSectionsPagerAdapter.editTabMode.value == true) {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            mAdapter.onItemMove(fromPosition, toPosition)
            list[fromPosition] = list[toPosition]
                    .also { list[toPosition] = list[fromPosition] }
            uiConfig.save()
        }
        return true
    }

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {

    }
}