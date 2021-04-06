package net.xzos.upgradeall.ui.base.selectlistdialog

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView


class SelectListTouchHelperCallBack(adapter: SelectListAdapter) : ItemTouchHelper.Callback() {
    private val mAdapter: SelectListAdapter = adapter

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags: Int = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags: Int = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        mAdapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }
}