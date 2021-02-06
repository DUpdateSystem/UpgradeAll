package net.xzos.upgradeall.ui.base.recycleview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.utils.runUiFun

abstract class RecyclerViewAdapter<L : ListItemView, T : RecyclerViewHolder<in L>> : RecyclerView.Adapter<T>() {

    lateinit var lifecycleScope: LifecycleCoroutineScope

    var dataSet: List<L> = listOf<L>().also { setHasStableIds(true) }
        set(value) {
            field = value
            runUiFun { notifyDataSetChanged() }
        }

    var mOnItemClickListener: ((view: View, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): T {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        return getViewHolder(layoutInflater, viewGroup)
    }

    abstract fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): T

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        val itemView = dataSet[position]
        viewHolder.bind(itemView)
        viewHolder.itemView.setOnClickListener {
            mOnItemClickListener?.run {
                this(it, position)
            }
        }
        lifecycleScope.launch {
            viewHolder.loadExtraUi(itemView)
        }
    }

    override fun getItemCount() = dataSet.size

    override fun getItemId(position: Int) = dataSet[position].hashCode().toLong()

    fun getItemData(position: Int) = dataSet[position]

    fun setOnItemClickListener(listener: (view: View, position: Int) -> Unit) {
        mOnItemClickListener = listener
    }
}