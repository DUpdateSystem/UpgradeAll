package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.json.gson.CloudConfig
import net.xzos.upgradeAll.json.nongson.ItemCardViewExtraData
import net.xzos.upgradeAll.server.hub.CloudHub
import net.xzos.upgradeAll.ui.viewmodels.adapters.CloudHubItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView

class HubCloudFragment : Fragment() {
    private val mCloudHub = CloudHub()

    private val itemCardViewList = ArrayList<ItemCardView>()
    private val adapter = CloudHubItemAdapter(itemCardViewList, mCloudHub)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cloud_hub_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        swipeRefresh.setOnRefreshListener { this.renewCardView() }
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.cloud_hub_list)
        renewCardView()
    }

    private fun renewCardView() {
        GlobalScope.launch {
            runBlocking(Dispatchers.Main) { swipeRefresh.isRefreshing = true }
            renewCloudHubList()
            runBlocking(Dispatchers.Main) { swipeRefresh.isRefreshing = false }
        }
    }

    private fun renewCloudHubList() {
        val isSuccess = mCloudHub.getCloudConfig()
        Handler(Looper.getMainLooper()).post {
            if (!isSuccess) {
                Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
            } else {
                val hubList = mCloudHub.hubList
                if (hubList != null) {
                    itemCardViewList.clear()
                    for (hubItem in hubList) {
                        getCloudHubItemCardView(hubItem)
                    }
                    if (itemCardViewList.isNotEmpty()) {
                        itemCardViewList.add(ItemCardView(Pair(null, null), null, null, ItemCardViewExtraData(isEmpty = true)))
                    }
                    setRecyclerView()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun getCloudHubItemCardView(item: CloudConfig.HubListBean): ItemCardView {
        val name = item.hubConfigName
        val hubUuid = item.hubConfigUuid
        val configFileName = item.hubConfigFileName
        val iconInfo: Pair<String?, String?> = Pair(configFileName, null)
        return ItemCardView(iconInfo, name, hubUuid, ItemCardViewExtraData(uuid = hubUuid, configFileName = configFileName))
    }

    private fun setRecyclerView() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        cardItemRecyclerView.adapter = adapter
    }
}