package net.xzos.upgradeall.ui.base.listdialog

import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.databinding.RecyclerlistContentBinding

interface ListDialogPart {
    val sAdapter: DialogListAdapter<*, *, *>

    fun initListView(contentBinding: RecyclerlistContentBinding) {
        contentBinding.apply {
            rvList.adapter = sAdapter
            rvList.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    fun renewListView(contentBinding: RecyclerlistContentBinding) {
        contentBinding.apply {
            if (sAdapter.itemCount > 0) {
                vfContainer.displayedChild = 0
            } else {
                vfContainer.displayedChild = 1
            }
        }
    }
}