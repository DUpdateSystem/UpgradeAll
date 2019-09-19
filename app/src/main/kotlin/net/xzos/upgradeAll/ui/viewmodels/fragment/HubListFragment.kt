package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.fragment_app_list.guidelinesTextView
import kotlinx.android.synthetic.main.fragment_hub_list.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.json.nongson.ItemCardViewExtraData
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.activity.HubCloudListActivity
import net.xzos.upgradeAll.ui.activity.HubDebugActivity
import net.xzos.upgradeAll.ui.viewmodels.adapters.LocalHubItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView

class HubListFragment : Fragment() {
    private val itemCardViewList = ArrayList<ItemCardView>()
    private val adapter = LocalHubItemAdapter(itemCardViewList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hub_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        swipeRefresh.setOnRefreshListener { this.refreshCardView() }
        setRecyclerView()
        addFab.addOnMenuItemClickListener { floatingActionButton, _, _ ->
            if (floatingActionButton === addFab.getMiniFab(0)) {
                startActivity(Intent(activity, HubDebugActivity::class.java))
            } else if (floatingActionButton === addFab.getMiniFab(1)) {
                startActivity(Intent(activity, HubCloudListActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.hub_list)
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
            adapter.notifyDataSetChanged()
        } else {
            guidelinesTextView.visibility = View.VISIBLE
        }
    }

    private fun setRecyclerView() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        cardItemRecyclerView.adapter = adapter
    }
}