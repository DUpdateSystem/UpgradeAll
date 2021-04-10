package net.xzos.upgradeall.ui.base.selectlistdialog

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import net.xzos.upgradeall.databinding.ItemEnableListBinding
import net.xzos.upgradeall.ui.base.listdialog.DialogListAdapter

class SelectListAdapter(val dataList: MutableList<SelectItem>)
    : DialogListAdapter<SelectItem, SelectItemHandler, SelectItemHolder>(dataList, SelectItemHandler(),
        fun(layoutInflater, viewGroup) = SelectItemHolder(ItemEnableListBinding.inflate(layoutInflater, viewGroup, false))
) {
    lateinit var itemTouchHelper: ItemTouchHelper

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SelectItemHolder {
        return super.onCreateViewHolder(viewGroup, viewType).apply {
            binding.ibDrag.setOnTouchListener { view, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }
                return@setOnTouchListener true
            }
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        dataList[toPosition] = dataList[fromPosition].also {
            dataList[fromPosition] = dataList[toPosition]
        }
        setDataList(dataList)
        notifyItemMoved(fromPosition, toPosition)
    }
}