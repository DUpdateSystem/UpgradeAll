package net.xzos.upgradeall.ui.viewmodels.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import net.xzos.upgradeall.ui.viewmodels.adapters.AppListItemAdapter
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
    }

    companion object {
        internal fun newInstance(tabPageIndex: Int) = AppListPlaceholderFragment(tabPageIndex)
    }
}
