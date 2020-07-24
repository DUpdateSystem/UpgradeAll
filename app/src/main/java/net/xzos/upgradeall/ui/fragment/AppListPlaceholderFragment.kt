package net.xzos.upgradeall.ui.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.content_list.*
import kotlinx.android.synthetic.main.layout_main.*
import kotlinx.android.synthetic.main.list_content.*
import kotlinx.android.synthetic.main.pageview_app_list.*
import kotlinx.android.synthetic.main.pageview_app_list.placeholderLayout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.database.AppDatabase
import net.xzos.upgradeall.core.server_manager.UpdateManager
import net.xzos.upgradeall.ui.activity.MainActivity
import net.xzos.upgradeall.ui.viewmodels.adapters.AppListItemAdapter
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel
import net.xzos.upgradeall.utils.IconPalette


internal class AppListPlaceholderFragment(private val tabPageIndex: Int)
    : AppListContainerFragment() {

    private lateinit var appListPageViewModel: AppListPageViewModel
    private lateinit var adapter: AppListItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            placeholderTextView.setText(R.string.waiting_check_update)
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
        appListPageViewModel.appCardViewList.observe(viewLifecycleOwner, Observer {
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
                fab.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showEditModeDialog() {
        context?.let {
            BottomSheetDialog(it).apply {
                setContentView(layoutInflater.inflate(R.layout.list_content, null))
                placeholderLayout.visibility = View.GONE
                val editModeList = listOf(
                        R.string.add_single_app,
                        R.string.add_applications
                ).map { resId ->
                    context.getString(resId)
                }
                list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, editModeList)
                list.setOnItemClickListener { _, _, position, _ ->
                    AppSettingFragment.bundleEditMode = when (editModeList[position]) {
                        context.getString(R.string.add_single_app) -> AppDatabase.APP_TYPE_TAG
                        context.getString(R.string.add_applications) -> AppDatabase.APPLICATIONS_TYPE_TAG
                        else -> null
                    }
                    MainActivity.setNavigationItemId(R.id.appSettingFragment)
                    cancel()
                }
            }.show()
        }
    }

    override fun renewAppList() {
        adapter.setItemList(appListPageViewModel.appCardViewList.value!!)
    }

    companion object {
        internal fun newInstance(tabPageIndex: Int) = AppListPlaceholderFragment(tabPageIndex)
    }
}
