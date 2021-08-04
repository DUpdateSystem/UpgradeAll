package net.xzos.upgradeall.ui.base.recycleview

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.xzos.upgradeall.ui.base.list.ListItemView
import net.xzos.upgradeall.utils.runUiFun

abstract class RecyclerViewAdapter<LT, L : ListItemView, RHA : RecyclerViewHandler, T : RecyclerViewHolder<in L, RHA, *>>(
    @Suppress("UNCHECKED_CAST")
    val listContainerViewConvertFun: (LT) -> L = { it as L }
) : RecyclerView.Adapter<T>() {

    abstract val handler: RHA?
    lateinit var lifecycleScope: LifecycleCoroutineScope

    private val dataSet: MutableList<LT> = mutableListOf<LT>().also { setHasStableIds(true) }

    fun setAdapterData(list: List<LT>, changedPosition: Int, changedTag: String) {
        dataSet.clear()
        dataSet.addAll(list)
        runUiFun {
            @SuppressLint("NotifyDataSetChanged")
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
        val itemView = listContainerViewConvertFun(dataSet[position])
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