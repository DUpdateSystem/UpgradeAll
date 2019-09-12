package net.xzos.upgradeAll.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.kobakei.materialfabspeeddial.FabSpeedDial
import kotlinx.android.synthetic.main.content_list.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.json.cache.ItemCardViewExtraData
import net.xzos.upgradeAll.server.hub.CloudHub
import net.xzos.upgradeAll.ui.viewmodels.adapters.CloudHubItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import java.util.*

class CloudHubListActivity : AppCompatActivity() {
    private val itemCardViewList = ArrayList<ItemCardView>()
    private lateinit var adapter: CloudHubItemAdapter

    private val cloudHub = CloudHub()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hub)
        // toolbar 点击事件
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        // 隐藏 tab
        val fab = findViewById<FabSpeedDial>(R.id.addFab)
        fab.visibility = View.GONE

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        swipeRefresh.setOnRefreshListener { this.refreshCardView() }

        setRecyclerView()
        refreshCardView()
    }

    private fun refreshCardView() {
        Thread {
            runOnUiThread {
                swipeRefresh.isRefreshing = true
            }
            refreshCloudHubList()
            runOnUiThread {
                swipeRefresh.isRefreshing = false
            }
        }.start()
    }

    private fun refreshCloudHubList() {
        val isSuccess = cloudHub.getCloudConfig()
        Handler(Looper.getMainLooper()).post {
            if (!isSuccess) {
                Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show()
            } else {
                val hubList = cloudHub.hubList
                if (hubList != null) {
                    itemCardViewList.clear()
                    for (hubItem in hubList) {
                        val name = hubItem.hubConfigName
                        val hubUuid = hubItem.hubConfigUuid
                        val configFileName = hubItem.hubConfigFileName
                        itemCardViewList.add(ItemCardView(name, hubUuid, configFileName, ItemCardViewExtraData(uuid = hubUuid, configFileName = configFileName)))
                    }
                    itemCardViewList.add(ItemCardView(null, null, null, ItemCardViewExtraData(isEmpty = true)))
                    setRecyclerView()
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setRecyclerView() {
        val layoutManager = GridLayoutManager(this, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        adapter = CloudHubItemAdapter(itemCardViewList, cloudHub)
        cardItemRecyclerView.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}