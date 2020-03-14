package net.xzos.upgradeall.ui.viewmodels.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.android.synthetic.main.content_list.*
import net.xzos.upgradeall.data_manager.UIConfig.Companion.uiConfig
import net.xzos.upgradeall.ui.viewmodels.adapters.AppListItemAdapter
import net.xzos.upgradeall.ui.viewmodels.callback.AppItemTouchHelperCallback
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.USER_STAR_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel


internal class AppListPlaceholderFragment(private val tabPageIndex: Int)
    : AppListContainerFragment() {

    private lateinit var appListPageViewModel: AppListPageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appListPageViewModel = ViewModelProvider(this).get(AppListPageViewModel::class.java)
        viewModel = appListPageViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appListPageViewModel.setTabPageIndex(tabPageIndex)  // 重新刷新跟踪项列表
        super.onViewCreated(view, savedInstanceState)
    }

    override fun renewAppList() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        val adapter = AppListItemAdapter(appListPageViewModel, appListPageViewModel.appCardViewList, this)
        cardItemRecyclerView.adapter = adapter
        if (tabPageIndex > 0 || tabPageIndex == USER_STAR_PAGE_INDEX) {
            val list = if (tabPageIndex > 0)
                uiConfig.userTabList[tabPageIndex].itemList
            else uiConfig.userStarTab.itemList
            val callback: ItemTouchHelper.Callback = AppItemTouchHelperCallback(adapter, list)
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(cardItemRecyclerView)
        }
    }

    companion object {
        internal fun newInstance(tabPageIndex: Int) = AppListPlaceholderFragment(tabPageIndex)
    }
}
