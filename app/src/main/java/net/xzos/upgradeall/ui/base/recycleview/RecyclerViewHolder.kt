package net.xzos.upgradeall.ui.base.recycleview

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.ui.base.list.ListItemView

abstract class RecyclerViewHolder<L : ListItemView, in H : RecyclerViewHandler, VB : ViewDataBinding>(protected val listBinding: VB, binding: ViewDataBinding)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(itemView: L) {
        doBind(itemView)
        listBinding.executePendingBindings()
    }

    abstract fun doBind(itemView: L)

    abstract fun setHandler(handler: H)

    open suspend fun loadExtraUi(itemView: L) {}
}