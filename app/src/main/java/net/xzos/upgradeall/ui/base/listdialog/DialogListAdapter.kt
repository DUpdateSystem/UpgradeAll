package net.xzos.upgradeall.ui.base.listdialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHandler
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

open class DialogListAdapter<L : ListItemView, RHA : RecyclerViewHandler, RH : RecyclerViewHolder<L, RHA, *>>(
        private var dataList: List<L>,
        private val handler: RHA? = null,
        private val getViewHolder: (LayoutInflater, ViewGroup) -> RH
) : RecyclerView.Adapter<RH>() {
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RH {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        return getViewHolder(layoutInflater, viewGroup).apply {
            handler?.let { this.setHandler(it) }
        }
    }

    override fun getItemCount() = dataList.size
    override fun onBindViewHolder(holder: RH, position: Int) {
        val itemView = dataList[position]
        holder.bind(itemView)
    }

    fun setDataList(list: List<L>) {
        dataList = list
    }
}