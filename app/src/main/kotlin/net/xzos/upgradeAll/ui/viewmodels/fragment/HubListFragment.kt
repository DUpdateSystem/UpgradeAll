package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.fragment_app_list.guidelinesTextView
import kotlinx.android.synthetic.main.fragment_hub_list.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.json.cache.ItemCardViewExtraData
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.activity.CloudHubListActivity
import net.xzos.upgradeAll.ui.activity.HubLocalActivity
import net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.adapters.LocalHubItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView

class HubListFragment : Fragment() {
    private val itemCardViewList = ArrayList<ItemCardView>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hub_list, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onResume() {
        super.onResume()
        addFab.addOnMenuItemClickListener { floatingActionButton, _, _ ->
            if (floatingActionButton === addFab.getMiniFab(0)) {
                startActivity(Intent(activity, HubLocalActivity::class.java))
            } else if (floatingActionButton === addFab.getMiniFab(1)) {
                startActivity(Intent(activity, CloudHubListActivity::class.java))
            }
        }
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        swipeRefresh.setOnRefreshListener { this.refreshCardView() }
        refreshCardView()
    }

    private fun refreshCardView() {
        swipeRefresh.isRefreshing = true
        refreshHubList()
        swipeRefresh.isRefreshing = false
    }

    private fun refreshHubList() {
        val hubDatabase = HubManager.databases
        itemCardViewList.clear()
        for (hubItem in hubDatabase) {
            val name = hubItem.name
            val uuid = hubItem.uuid
            itemCardViewList.add(ItemCardView(name, uuid, "", ItemCardViewExtraData(uuid = uuid)))
        }
        if (itemCardViewList.size != 0) {
            itemCardViewList.add(ItemCardView(null, null, null, ItemCardViewExtraData(isEmpty = true)))
            guidelinesTextView.visibility = View.GONE
            setRecyclerView()
        } else {
            guidelinesTextView.visibility = View.VISIBLE
        }
    }

    private fun setRecyclerView() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        val adapter = LocalHubItemAdapter(itemCardViewList)
        cardItemRecyclerView.adapter = adapter
        adapter.notifyDataSetChanged()
    }
}