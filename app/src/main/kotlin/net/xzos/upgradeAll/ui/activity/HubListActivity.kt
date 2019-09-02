package net.xzos.upgradeAll.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.kobakei.materialfabspeeddial.FabSpeedDial
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.json.cache.ItemCardViewExtraData
import net.xzos.upgradeAll.server.hub.HubManager
import net.xzos.upgradeAll.ui.viewmodels.adapters.LocalHubItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import java.util.*

class HubListActivity : AppCompatActivity() {
    private val itemCardViewList = ArrayList<ItemCardView>()
    private lateinit var adapter: LocalHubItemAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onResume() {
        super.onResume()
        refreshHubList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hub)
        // toolbar 点击事件
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        // tab添加事件
        val fab = findViewById<FabSpeedDial>(R.id.addFab)
        fab.addOnMenuItemClickListener { floatingActionButton, _, _ ->
            if (floatingActionButton === fab.getMiniFab(0)) {
                startActivity(Intent(this, HubLocalActivity::class.java))
            } else if (floatingActionButton === fab.getMiniFab(1)) {
                startActivity(Intent(this@HubListActivity, CloudHubListActivity::class.java))
            }
        }

        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        swipeRefresh.setOnRefreshListener { this.refreshCardView() }

        recyclerView = findViewById(R.id.update_item_recycler_view)

        setRecyclerView()
    }

    private fun refreshCardView() {
        Thread {
            runOnUiThread {
                swipeRefresh.isRefreshing = true
                refreshHubList()
                swipeRefresh.isRefreshing = false
            }
        }.start()
    }

    private fun refreshHubList() {
        val hubDatabase = HubManager.databases
        itemCardViewList.clear()
        for (hubItem in hubDatabase) {
            val name = hubItem.name
            val uuid = hubItem.uuid
            itemCardViewList.add(ItemCardView(name, uuid, "", ItemCardViewExtraData(uuid = uuid)))
        }
        val guidelinesTextView = findViewById<TextView>(R.id.guidelinesTextView)
        if (itemCardViewList.size != 0) {
            itemCardViewList.add(ItemCardView(null, null, null, ItemCardViewExtraData(isEmpty = true)))
            setRecyclerView()
            adapter.notifyDataSetChanged()
            guidelinesTextView.visibility = View.GONE
        } else {
            guidelinesTextView.visibility = View.VISIBLE
        }
    }

    private fun setRecyclerView() {
        val layoutManager = GridLayoutManager(this, 1)
        recyclerView.layoutManager = layoutManager
        adapter = LocalHubItemAdapter(itemCardViewList)
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_actionbar_hub, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.app_help -> {
                var intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://xzos.net/upgradeall-developer-documentation/")
                intent = Intent.createChooser(intent, "请选择浏览器以查看帮助文档")
                startActivity(intent)
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
