package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.os.Bundle
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cloud_hub_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener { this.renewCardView() }
        renewCardView()
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.cloud_hub_list)
    }

    private fun renewCardView() {
        GlobalScope.launch {
            runBlocking(Dispatchers.Main) { swipeRefreshLayout?.isRefreshing = true }
            renewCloudHubList()
            runBlocking(Dispatchers.Main) { swipeRefreshLayout?.isRefreshing = false }
        }
    }

    private fun renewCloudHubList() {
        val itemCardViewList = mCloudHub.hubList?.map { getCloudHubItemCardView(it) }
                ?.plus(ItemCardView(Pair(null, null), null, null, ItemCardViewExtraData(isEmpty = true)))
                ?: runBlocking(Dispatchers.Main) {
                    Toast.makeText(activity, "网络错误", Toast.LENGTH_SHORT).show()
                    return@runBlocking listOf<ItemCardView>()
                }
        runBlocking(Dispatchers.Main) {
            val layoutManager = GridLayoutManager(activity, 1)
            cardItemRecyclerView?.layoutManager = layoutManager
            val adapter = CloudHubItemAdapter(itemCardViewList, mCloudHub)
            cardItemRecyclerView?.adapter = adapter
        }
    }

    private fun getCloudHubItemCardView(item: CloudConfig.HubListBean): ItemCardView {
        val name = item.hubConfigName
        val hubUuid = item.hubConfigUuid
        val configFileName = item.hubConfigFileName
        val iconInfo: Pair<String?, String?> = Pair(configFileName, null)
        return ItemCardView(iconInfo, name, hubUuid, ItemCardViewExtraData(uuid = hubUuid, configFileName = configFileName))
    }
}