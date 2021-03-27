package net.xzos.upgradeall.ui.base.recycleview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.utils.runUiFun

abstract class RecyclerViewAdapter<L : ListItemView, RHA : RecyclerViewHandler, T : RecyclerViewHolder<in L, RHA, *>> : RecyclerView.Adapter<T>() {

    abstract val handler: RHA?
    lateinit var lifecycleScope: LifecycleCoroutineScope

    private var dataSet: List<L> = listOf<L>().also { setHasStableIds(true) }

    fun setAdapterData(list: List<L>, changedPosition: Int, changedTag: String) {
        dataSet = list
        runUiFun {
            when (changedTag) {
                RENEW -> notifyDataSetChanged()
                ADD -> notifyItemInserted(changedPosition)
                DEL -> notifyItemRemoved(changedPosition)
                CHANGE -> notifyItemChanged(changedPosition)
                else -> notifyDataSetChanged()

            }
        }
    }

    fun getAdapterData() = dataSet

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): T {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        return getViewHolder(layoutInflater, viewGroup).apply {
            handler?.let { this.setHandler(it) }
        }
    }

    abstract fun getViewHolder(layoutInflater: LayoutInflater, viewGroup: ViewGroup): T

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        val itemView = dataSet[position]
        viewHolder.bind(itemView)
        lifecycleScope.launch {
            viewHolder.loadExtraUi(itemView)
        }
    }

    override fun getItemCount() = dataSet.size

    override fun getItemId(position: Int) = dataSet[position].hashCode().toLong()

    fun getItemData(position: Int) = dataSet[position]

    companion object {
        const val ADD = "ADD"
        const val DEL = "DEL"
        const val CHANGE = "CHANGE"
        const val RENEW = "RENEW"
    }
}