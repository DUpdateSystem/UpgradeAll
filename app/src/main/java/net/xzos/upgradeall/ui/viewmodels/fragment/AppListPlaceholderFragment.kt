package net.xzos.upgradeall.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.pageview_app_list.*
import kotlinx.android.synthetic.main.pageview_app_list.view.*
import net.xzos.dupdatesystem.core.server_manager.runtime.manager.module.app.App
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.viewmodels.adapters.AppItemAdapter
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel

internal class AppListPlaceholderFragment(private val tabPageIndex: Int) : Fragment() {

    private lateinit var appListPageViewModel: AppListPageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appListPageViewModel = ViewModelProvider(this).get(AppListPageViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.pageview_app_list, container, false).apply {
                this.placeholderLayout.visibility = View.VISIBLE
            }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener { renewPage() }
        // TODO: 修改逻辑
        context?.getString(R.string.example_update_overview)
                ?.split("0")
                ?.filter { element -> element.isNotBlank() }
                ?.let { updateOverviewStringList ->
                    var appListNum = 0
                    var needUpdateAppNum = 0
                    appListPageViewModel.appCardViewList.observe(viewLifecycleOwner, Observer { list ->
                        with(list.size) {
                            appListNum = if (this > 0)
                                this - 1
                            else
                                this
                        }
                        updateOverviewTextView.text = "$appListNum${updateOverviewStringList[0]}$needUpdateAppNum${updateOverviewStringList[1]}"
                    })
                    appListPageViewModel.needUpdateAppsLiveLiveData.observe(viewLifecycleOwner,
                            Observer<MutableList<App>> { list ->
                                needUpdateAppNum = list.size
                                updateOverviewTextView.text = "$appListNum${updateOverviewStringList[0]}$needUpdateAppNum${updateOverviewStringList[1]}"
                                if (needUpdateAppNum == 0) {
                                    updateOverviewStatusImageView.setImageResource(R.drawable.ic_check_mark)
                                    updateOverviewNumberTextView.visibility = View.GONE
                                } else {
                                    updateOverviewStatusImageView.setImageResource(R.drawable.ic_up)
                                    updateOverviewNumberTextView.visibility = View.VISIBLE
                                    updateOverviewNumberTextView.text = needUpdateAppNum.toString()
                                }
                            })
                }
        renewPage()
    }

    private fun renewCardView() {
        swipeRefreshLayout?.isRefreshing = true
        renewAppList()
        swipeRefreshLayout?.isRefreshing = false
    }

    private fun renewAppList() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        val adapter = AppItemAdapter(appListPageViewModel, appListPageViewModel.appCardViewList, this)
        cardItemRecyclerView.adapter = adapter
    }

    private fun renewPage() {
        appListPageViewModel.setTabPageIndex(tabPageIndex)  // 重新刷新跟踪项列表
        appListPageViewModel.appCardViewList.observe(viewLifecycleOwner, Observer {
            // 列表显示刷新
            if (it.isNullOrEmpty()) {
                updateOverviewLayout.visibility = View.GONE
                placeholderLayout.visibility = View.VISIBLE
                placeholderImageVew.setImageResource(R.drawable.ic_isnothing_placeholder)
                with(placeholderTextView) {
                    text = this.context.getText(R.string.click_to_add_something)
                }
            } else {
                updateOverviewLayout.visibility = View.VISIBLE
                placeholderLayout.visibility = View.GONE
                renewCardView()
            }

        })
        updateOverviewLayout.visibility = View.VISIBLE
        placeholderLayout.visibility = View.GONE
    }

    companion object {

        internal fun newInstance(tabPageIndex: Int): AppListPlaceholderFragment =
                AppListPlaceholderFragment(tabPageIndex)
    }
}
