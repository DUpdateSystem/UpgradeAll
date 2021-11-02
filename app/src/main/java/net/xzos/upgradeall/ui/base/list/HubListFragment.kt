package net.xzos.upgradeall.ui.base.list

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

abstract class HubListFragment<T, L : ListItemView, RH : RecyclerViewHolder<L, *, *>> :
    HubListPart<T, L, RH>, Fragment() {
    override lateinit var rvList: RecyclerView
    override var srlContainer: SwipeRefreshLayout? = null

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    fun initView(rvList: RecyclerView, srlContainer: SwipeRefreshLayout? = null) {
        this.rvList = rvList
        this.srlContainer = srlContainer
        initViewData(viewLifecycleOwner)
    }
}