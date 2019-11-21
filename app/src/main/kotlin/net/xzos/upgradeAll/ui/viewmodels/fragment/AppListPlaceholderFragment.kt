package net.xzos.upgradeAll.ui.viewmodels.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.pageview_app_list.*
import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.ui.viewmodels.adapters.AppItemAdapter
import net.xzos.upgradeAll.ui.viewmodels.viewmodel.AppListPageViewModel

internal class AppListPlaceholderFragment : Fragment() {

    private lateinit var appListPageViewModel: AppListPageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appListPageViewModel = ViewModelProvider(this).get(AppListPageViewModel::class.java)
        val hubUuid = arguments?.getString(ARG_SECTION_NUMBER)
        if (hubUuid != null)
            appListPageViewModel.setHubUuid(hubUuid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.pageview_app_list, container, false).apply {
                this.findViewById<LinearLayout>(R.id.placeholderLayout).visibility = View.VISIBLE
            }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary)
        swipeRefreshLayout.setOnRefreshListener { this.renewCardView() }
        with(updateOverviewTextView) {
            val updateOverviewStringList = this.context.getString(R.string.example_update_overview).split("0")
                    .filter { element -> element.isNotBlank() }
            var appListNum = 0
            var needUpdateAppNum = 0
            appListPageViewModel.appCardViewList.observe(viewLifecycleOwner, Observer { list ->
                with(list.size) {
                    appListNum = if (this > 0)
                        this - 1
                    else
                        this
                }
                this.text = "$appListNum${updateOverviewStringList[0]}$needUpdateAppNum${updateOverviewStringList[1]}"
            })
            appListPageViewModel.needUpdateAppIdLiveLiveData.observe(viewLifecycleOwner,
                    Observer<MutableList<Long>> { list ->
                        needUpdateAppNum = list.size
                        this.text = "$appListNum${updateOverviewStringList[0]}$needUpdateAppNum${updateOverviewStringList[1]}"
                    })
        }
        renewCardView()
    }

    private fun renewCardView() {
        swipeRefreshLayout?.isRefreshing = true
        renewAppList()
        swipeRefreshLayout?.isRefreshing = false
    }

    private fun renewAppList() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        val adapter = AppItemAdapter(appListPageViewModel.needUpdateAppIdLiveLiveData, appListPageViewModel.appCardViewList, this)
        cardItemRecyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // 占位符修改
        appListPageViewModel.appCardViewList.value?.let {
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
            }
        }
    }

    companion object {

        private const val ARG_SECTION_NUMBER = "hubUuidTag"

        internal fun newInstance(hubUuid: String): AppListPlaceholderFragment {
            val fragment = AppListPlaceholderFragment()
            val bundle = Bundle()
            bundle.putString(ARG_SECTION_NUMBER, hubUuid)
            fragment.arguments = bundle
            return fragment
        }
    }
}