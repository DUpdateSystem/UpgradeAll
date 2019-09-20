package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.fragment_app_list.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.database.RepoDatabase
import net.xzos.upgradeAll.json.nongson.ItemCardViewExtraData
import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.callback.AppItemTouchHelperCallback
import net.xzos.upgradeAll.ui.viewmodels.view.ItemCardView
import org.litepal.LitePal
import org.litepal.extension.find

class AppListFragment : Fragment() {
    private val itemCardViewList = ArrayList<ItemCardView>()
    private val adapter = AppItemAdapter(itemCardViewList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary)
        swipeRefresh.setOnRefreshListener { this.refreshCardView() }
        setRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        activity?.findViewById<NavigationView>(R.id.navView)?.setCheckedItem(R.id.app_list)
        refreshCardView()
    }

    private fun refreshCardView() {
        swipeRefresh.isRefreshing = true
        refreshAppList()
        swipeRefresh.isRefreshing = false
    }

    private fun getItemCardView(item: RepoDatabase): ItemCardView {
        val databaseId = item.id
        val name = item.name
        val api = item.api
        val url = item.url
        return ItemCardView(name, url, api, ItemCardViewExtraData(databaseId = databaseId))
    }

    private fun refreshAppList() {
        fun addItemCardView(id: Long): Boolean {
            val updateItem: RepoDatabase? = LitePal.find(id)
            if (updateItem != null) {
                itemCardViewList.add(getItemCardView(updateItem))
                return true
            }
            return false
        }

        val repoDatabase = LitePal.findAll(RepoDatabase::class.java)
        val appDatabaseIdList = mutableListOf<Long>()
        for (app in repoDatabase) {
            appDatabaseIdList.add(app.id)
        }
        itemCardViewList.clear()
        val uiConfig = ServerContainer.UIConfig
        val appList = uiConfig.appList
        for (id in appList) {
            if (id is Long)
                if (addItemCardView(id))
                    appDatabaseIdList.remove(id)
        }
        if (appDatabaseIdList.isNotEmpty()) {
            for (id in appDatabaseIdList) {
                addItemCardView(id)
                appList.add(id)
            }
            uiConfig.save()
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
        val itemTouchHelperCallback = AppItemTouchHelperCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(cardItemRecyclerView)
    }
}