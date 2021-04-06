package net.xzos.upgradeall.ui.base.selectlistdialog

import net.xzos.upgradeall.databinding.ItemEnableListBinding
import net.xzos.upgradeall.ui.base.listdialog.DialogListAdapter

class SelectListAdapter(val dataList: MutableList<SelectItem>)
    : DialogListAdapter<SelectItem, SelectItemHandler, SelectItemHolder>(dataList, SelectItemHandler(),
        fun(layoutInflater, viewGroup) = SelectItemHolder(ItemEnableListBinding.inflate(layoutInflater, viewGroup, false))
) {
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        dataList[toPosition] = dataList[fromPosition].also {
            dataList[fromPosition] = dataList[toPosition]
        }
        setDataList(dataList)
        notifyItemMoved(fromPosition, toPosition)
    }
}