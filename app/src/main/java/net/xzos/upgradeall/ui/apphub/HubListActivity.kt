package net.xzos.upgradeall.ui.apphub

import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import com.absinthe.libraries.utils.extensions.addPaddingBottom
import com.absinthe.libraries.utils.utils.UiUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.ActivityDiscoverBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.ui.viewmodels.view.ListItemView
import net.xzos.upgradeall.ui.viewmodels.view.RecyclerViewAdapter
import net.xzos.upgradeall.ui.viewmodels.view.holder.RecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.ListContainerViewModel

abstract class HubListActivity<L : ListItemView, T : RecyclerViewHolder<L>> : AppBarActivity(), SearchView.OnQueryTextListener {
    protected lateinit var binding: ActivityDiscoverBinding
    protected abstract val adapter: RecyclerViewAdapter<L, T>
    protected abstract val viewModel: ListContainerViewModel<L>
    private var isListReady = false
    private var menu: Menu? = null

    override fun initView() {
        binding.rvList.apply {
            adapter = this@HubListActivity.adapter
            addPaddingBottom(UiUtils.getNavBarHeight(contentResolver))
        }
        viewModel.getList().observe(this) {
            adapter.dataSet = it
            binding.srlContainer.isRefreshing = false
            isListReady = true
            menu?.findItem(R.id.search)?.isVisible = true
        }
        binding.srlContainer.apply {
            setProgressBackgroundColorSchemeResource(R.color.colorPrimary)
            setColorSchemeColors(Color.WHITE)
            setOnRefreshListener {
                refreshList()
            }
        }
        refreshList()
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initBinding(): View {
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun refreshList() {
        binding.srlContainer.isRefreshing = true
        viewModel.loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_cloud_app_list, menu)
        this.menu = menu

        val searchView = SearchView(this@HubListActivity).apply {
            setIconifiedByDefault(false)
            setOnQueryTextListener(this@HubListActivity)
            queryHint = getText(R.string.menu_search_hint)
            isQueryRefinementEnabled = true

            findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
        }

        menu.findItem(R.id.search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
            actionView = searchView

            if (!isListReady) {
                isVisible = false
            }
        }

        return true
    }

    override fun onQueryTextSubmit(query: String) = false

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.getList().value?.let { list ->
            val filter = list.filter {
                it.name.contains(newText, ignoreCase = true)
            }
            adapter.dataSet = filter
            adapter.notifyDataSetChanged()
        }
        return false
    }
}