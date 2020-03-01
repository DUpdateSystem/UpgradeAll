package net.xzos.upgradeall.ui.viewmodels.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel


internal class AppListPlaceholderFragment(private val tabPageIndex: Int) : AppListContainerFragment() {

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

    companion object {
        internal fun newInstance(tabPageIndex: Int) = AppListPlaceholderFragment(tabPageIndex)
    }
}
