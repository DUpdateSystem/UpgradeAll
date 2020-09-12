package net.xzos.upgradeall.ui.fragment.app_list.page_view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.fragment_applications.*
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.adapters.ApplicationsItemAdapter
import net.xzos.upgradeall.ui.viewmodels.viewmodel.ApplicationsPageViewModel

/**
 * 应用市场详细数据展示页面
 * 作为框架嵌套到 主页[net.xzos.upgradeall.ui.activity.MainActivity]
 * 由点击 更新项 [net.xzos.upgradeall.ui.viewmodels.adapters.AppListItemAdapter] 动作 触发显示
 * 使用 [net.xzos.upgradeall.ui.activity.MainActivity.setFrameLayout] 方法跳转
 */
class ApplicationsFragment : AppListContainerFragment() {
    private lateinit var applications: Applications
    private lateinit var applicationsPageViewModel: ApplicationsPageViewModel
    private lateinit var adapter: ApplicationsItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationsPageViewModel = ViewModelProvider(this).get(ApplicationsPageViewModel::class.java)
        adapter = ApplicationsItemAdapter(applicationsPageViewModel)
        viewModel = applicationsPageViewModel
        MainActivity.actionBarDrawerToggle.isDrawerIndicatorEnabled = false  // 禁止开启侧滑栏，启用返回按钮响应事件
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAppListAdapter()
    }

    override fun onResume() {
        checkAppInfo()
        super.onResume()
    }

    private fun initAppListAdapter() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        val adapter = ApplicationsItemAdapter(applicationsPageViewModel)
        cardItemRecyclerView.adapter = adapter
        viewModel.appCardViewList.observe(viewLifecycleOwner, {
            adapter.setItemList(it)
        })
    }

    private fun checkAppInfo() {
        bundleApplications?.run {
            applications = this
            nameTextView.text = applications.name
            applicationsPageViewModel.setApplications(applications)
        } ?: activity?.onBackPressed()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_applications, container, false)
    }

    override fun renewAppList() {
        adapter.setItemList(viewModel.appCardViewList.value!!)
    }

    companion object {
        // 显示的应用市场对象
        var bundleApplications: Applications? = null
    }
}
