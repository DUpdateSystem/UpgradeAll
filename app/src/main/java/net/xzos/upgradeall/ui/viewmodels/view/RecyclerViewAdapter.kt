package net.xzos.upgradeall.ui.viewmodels.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.xzos.upgradeall.ui.viewmodels.view.holder.RecyclerViewHolder

abstract class RecyclerViewAdapter<L : ListItemView, T : RecyclerViewHolder<L>>(
        var dataSet: List<L> = emptyList()
) : RecyclerView.Adapter<T>() {

    var mOnItemClickListener: ((view: View, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): T {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        return getViewHolder(layoutInflater, viewGroup).apply {
            itemView.setOnClickListener {

            }
        }
    }

    abstract fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): T

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        viewHolder.bind(dataSet[position])
        viewHolder.itemView.setOnClickListener {
            mOnItemClickListener?.run {
                this(it, position)
            }
        }
    }

    override fun getItemCount() = dataSet.size

    fun getItemData(position: Int) = dataSet[position]

    fun setOnItemClickListener(listener: (view: View, position: Int) -> Unit) {
        mOnItemClickListener = listener
    }
}