package net.xzos.upgradeall.ui.fragment.app_list.page_view

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.android.synthetic.main.pageview_app_list.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.ui.detail.setting.AppSettingActivity
import net.xzos.upgradeall.ui.detail.setting.ApplicationsSettingActivity
import net.xzos.upgradeall.ui.viewmodels.adapters.AppListItemAdapter
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel
import net.xzos.upgradeall.utils.IconPalette


internal class AppListPlaceholderFragment : AppListContainerFragment() {

    private var tabPageIndex: Int = 0
    private lateinit var appListPageViewModel: AppListPageViewModel
    private lateinit var adapter: AppListItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabPageIndex = arguments?.getInt(TAB_PAGE_INDEX) ?: 0
        appListPageViewModel = ViewModelProvider(this).get(AppListPageViewModel::class.java)
        adapter = AppListItemAdapter(appListPageViewModel)
        viewModel = appListPageViewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appListPageViewModel.setTabPageIndex(tabPageIndex)  // 重新刷新跟踪项列表
        super.onViewCreated(view, savedInstanceState)
        initUi()
        initAppListAdapter()
    }

    override fun onResume() {
        super.onResume()
        setFloatingActionButton(tabPageIndex)
    }

    private fun initUi() {
        if (appListPageViewModel.getTabPageIndex() == UPDATE_PAGE_INDEX) {
            placeholderImageVew.setImageResource(R.drawable.ic_checking_update)
            placeholderTextView.visibility = View.GONE
        }
    }

    private fun initAppListAdapter() {
        val layoutManager = GridLayoutManager(activity, 1)
        cardItemRecyclerView.layoutManager = layoutManager
        /*
        val callback: ItemTouchHelper.Callback = AppItemTouchHelperCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(cardItemRecyclerView)
        无用手势代码
         */
        cardItemRecyclerView.adapter = adapter
        appListPageViewModel.appCardViewList.observe(viewLifecycleOwner, {
            adapter.setItemList(it)
        })
    }

    private fun setFloatingActionButton(tabPageIndex: Int) {
        activity?.run {
            addFloatingActionButton.let { fab ->
                if (tabPageIndex == UPDATE_PAGE_INDEX) {
                    fab.setOnClickListener {
                        GlobalScope.launch { UpdateManager.downloadAllUpdate() }
                    }
                    fab.setImageDrawable(IconPalette.fabUpdateIcon)
                    fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.colorDarkAccent)))
                } else {
                    fab.setOnClickListener { showEditModeDialog() }
                    fab.setImageDrawable(IconPalette.fabAddIcon)
                    fab.backgroundTintList = ColorStateList.valueOf((IconPalette.getColorInt(R.color.bright_yellow)))
                }
                fab.setColorFilter(IconPalette.getColorInt(R.color.light_gray))
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showEditModeDialog() {
        context?.let {
            val editModeList = arrayOf<CharSequence>(
                    getString(R.string.add_single_app), getString(R.string.add_applications)
            )
            AlertDialog.Builder(it)
                    .setItems(editModeList) { _, which ->
                        when (editModeList[which]) {
                            getString(R.string.add_single_app) ->
                                AppSettingActivity.getInstance(requireContext(), null)
                            getString(R.string.add_applications) ->
                                ApplicationsSettingActivity.getInstance(requireContext(), null)
                        }
                    }
                    .create()
                    .show()
        }
    }

    override fun renewAppList() {
        adapter.setItemList(mutableListOf())
        adapter.setItemList(appListPageViewModel.appCardViewList.value!!)
    }

    companion object {
        private const val TAB_PAGE_INDEX = "TAB_PAGE_INDEX"
        internal fun newInstance(tabPageIndex: Int): AppListPlaceholderFragment {
            val args = Bundle()
            args.putInt(TAB_PAGE_INDEX, tabPageIndex)
            return AppListPlaceholderFragment().apply {
                arguments = args
            }
        }
    }
}
