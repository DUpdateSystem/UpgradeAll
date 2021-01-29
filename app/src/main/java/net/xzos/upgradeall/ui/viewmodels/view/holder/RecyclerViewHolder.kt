package net.xzos.upgradeall.ui.viewmodels.view.holder

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.ui.viewmodels.view.ListItemView

abstract class RecyclerViewHolder<L : ListItemView>(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(itemView: L) {
        doBind(itemView)
        binding.executePendingBindings()
    }

    abstract fun doBind(itemView: L)
}