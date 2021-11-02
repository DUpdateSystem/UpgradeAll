package net.xzos.upgradeall.ui.base.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import net.xzos.upgradeall.databinding.FragmentHubListBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

abstract class HubListFragment<T, L : ListItemView, RH : RecyclerViewHolder<L, *, *>> :
    HubListPart<T, L, RH>, Fragment() {
    override lateinit var rvList: RecyclerView
    override var srlContainer: SwipeRefreshLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentHubListBinding.inflate(inflater).also {
            initView(it.listLayout.rvList, it.listLayout.srlContainer)
        }.root
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun initView(rvList: RecyclerView, srlContainer: SwipeRefreshLayout? = null) {
        this.rvList = rvList
        this.srlContainer = srlContainer
        initViewData(viewLifecycleOwner)
    }
}