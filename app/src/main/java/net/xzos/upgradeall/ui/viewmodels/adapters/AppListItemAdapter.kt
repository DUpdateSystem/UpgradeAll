package net.xzos.upgradeall.ui.viewmodels.adapters

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.ui.activity.MainActivity.Companion.setNavigationItemId
import net.xzos.upgradeall.ui.fragment.AppInfoFragment
import net.xzos.upgradeall.ui.fragment.app_list.page_view.ApplicationsFragment
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.ALL_APP_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter.Companion.UPDATE_PAGE_INDEX
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.getByHolder


class AppListItemAdapter(private val appListPageViewModel: AppListPageViewModel
) : AppItemAdapter(appListPageViewModel.appCardViewList.value ?: mutableListOf(),
        appListPageViewModel.needUpdateAppsLiveData) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        // 单击展开 Release 详情页
        holder.itemCardView.setOnClickListener {
            if (holder.adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            when (val baseApp = mItemCardViewList.getByHolder(holder).extraData.app) {
                is App -> {
                    AppInfoFragment.bundleApp = baseApp
                    setNavigationItemId(R.id.appInfoFragment)
                }
                is Applications -> {
                    ApplicationsFragment.bundleApplications = baseApp
                    setNavigationItemId(R.id.applicationsFragment)
                }
            }
        }
        holder.itemCardView.setOnLongClickListener { view ->
            if (holder.adapterPosition == RecyclerView.NO_POSITION) return@setOnLongClickListener false
            if (AppTabSectionsPagerAdapter.editTabMode.value == false
                    && appListPageViewModel.getTabPageIndex() != UPDATE_PAGE_INDEX)
                showLongClickPopMenu(view, holder)
            return@setOnLongClickListener true
        }
        return holder
    }

    private fun showLongClickPopMenu(view: View, holder: CardViewRecyclerViewHolder) {
        mItemCardViewList.getByHolder(holder).extraData.app?.run {
            val context = view.context
            PopupMenu(context, view).let { popupMenu ->
                popupMenu.menu.let { menu ->
                    menu.add(context.getString(
                            if (appListPageViewModel.editableTab.value == true)
                                R.string.edit_group
                            else R.string.add_to_group
                    )).let { menuItem ->
                        menuItem.setOnMenuItemClickListener {
                            showSelectGroupPopMenu(view, holder)
                            return@setOnMenuItemClickListener true
                        }
                    }
                    // 从分组中删除
                    val tabPageIndex = appListPageViewModel.getTabPageIndex()
                    if (tabPageIndex != ALL_APP_PAGE_INDEX
                            && tabPageIndex != UPDATE_PAGE_INDEX) {
                        menu.add(R.string.delete_from_group).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                if (appListPageViewModel.removeItemFromTabPage(holder.adapterPosition))
                                    onItemDismiss(holder.adapterPosition)
                                return@setOnMenuItemClickListener true
                            }
                        }
                    }
                    // 导出
                    menu.add(R.string.export).let { menuItem ->
                        menuItem.setOnMenuItemClickListener {
                            val appConfigGson = AppDatabaseManager.translateAppConfig(this.appDatabase)
                            FileUtil.clipStringToClipboard(
                                    GsonBuilder().setPrettyPrinting().create().toJson(appConfigGson),
                                    context
                            )
                            return@setOnMenuItemClickListener true
                        }
                    }
                    // 删除数据库
                    menu.add(R.string.delete).let { menuItem ->
                        menuItem.setOnMenuItemClickListener {
                            if (this.appDatabase.delete() && appListPageViewModel.removeItemFromTabPage(holder.adapterPosition))
                                onItemDismiss(holder.adapterPosition)
                            return@setOnMenuItemClickListener true
                        }
                    }
                    popupMenu.show()
                }
            }
        }
    }

    private fun showSelectGroupPopMenu(view: View, holder: CardViewRecyclerViewHolder) {
        PopupMenu(view.context, view).let { popupMenu ->
            popupMenu.menu.let { menu ->
                val tabInfoList = appListPageViewModel.getTabIndexList()
                for ((tabIndex, tabInfo) in tabInfoList)
                    menu.add(tabInfo.name).let { menuItem: MenuItem ->
                        menuItem.setOnMenuItemClickListener {
                            appListPageViewModel.moveItemToOtherTabPage(holder.adapterPosition, tabIndex)
                            return@setOnMenuItemClickListener true
                        }
                    }
            }
            popupMenu.show()
        }
    }
}
