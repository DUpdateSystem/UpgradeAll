package net.xzos.upgradeall.ui.viewmodels.adapters

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.dupdatesystem.core.data_manager.AppDatabaseManager
import net.xzos.dupdatesystem.core.server_manager.module.app.App
import net.xzos.dupdatesystem.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.activity.MainActivity.Companion.setNavigationItemId
import net.xzos.upgradeall.ui.viewmodels.fragment.AppInfoFragment
import net.xzos.upgradeall.ui.viewmodels.fragment.ApplicationsFragment
import net.xzos.upgradeall.ui.viewmodels.pageradapter.AppTabSectionsPagerAdapter
import net.xzos.upgradeall.ui.viewmodels.view.ItemCardView
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.AppListPageViewModel
import net.xzos.upgradeall.utils.FileUtil
import net.xzos.upgradeall.utils.getByHolder


class AppListItemAdapter(private val appListPageViewModel: AppListPageViewModel,
                         itemCardViewLiveData: LiveData<MutableList<ItemCardView>>,
                         owner: LifecycleOwner)
    : AppItemAdapter(appListPageViewModel, itemCardViewLiveData, owner) {

    private var mItemCardViewList: MutableList<ItemCardView> = mutableListOf()
    // TODO: 数据无法自动更新（需修复）

    init {
        itemCardViewLiveData.observe(owner, Observer { list ->
            mItemCardViewList = list
            notifyDataSetChanged()
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        // 单击展开 Release 详情页
        holder.itemCardView.setOnClickListener {
            val app = mItemCardViewList.getByHolder(holder).extraData.app
            if (app is App) {
                AppInfoFragment.bundleApp = app
                setNavigationItemId(R.id.appInfoFragment)
            } else if (app is Applications) {
                ApplicationsFragment.bundleApplications = app
                setNavigationItemId(R.id.applicationsFragment)
            }
        }
        // TODO: 长按删除，暂时添加删除功能
        holder.itemCardView.setOnLongClickListener { view ->
            if (AppTabSectionsPagerAdapter.editTabMode.value == false)
                GlobalScope.launch {
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
                                        GlobalScope.launch(Dispatchers.Main) {
                                            showSelectGroupPopMenu(view, holder)
                                        }
                                        return@setOnMenuItemClickListener true
                                    }
                                }
                                // 从分组中删除
                                if (appListPageViewModel.editableTab.value == true) {
                                    menu.add(R.string.delete_from_group).let { menuItem ->
                                        menuItem.setOnMenuItemClickListener {
                                            if (appListPageViewModel.removeItemFromGroup(holder.adapterPosition))
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
                                        this.appDatabase.delete()
                                        onItemDismiss(holder.adapterPosition)
                                        return@setOnMenuItemClickListener true
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    popupMenu.show()
                                }
                            }
                        }
                    }
                }
            return@setOnLongClickListener true
        }
        return holder
    }

    private fun showSelectGroupPopMenu(view: View, holder: CardViewRecyclerViewHolder) {
        PopupMenu(view.context, view).let { popupMenu ->
            popupMenu.menu.let { menu ->
                val tabInfoList = appListPageViewModel.getTabIndexList()
                for (containerTabListBean in tabInfoList)
                    menu.add(containerTabListBean.name).let { menuItem: MenuItem ->
                        menuItem.setOnMenuItemClickListener {
                            appListPageViewModel.moveItemToOtherGroup(holder.adapterPosition, containerTabListBean)
                            return@setOnMenuItemClickListener true
                        }
                    }
            }
            popupMenu.show()
        }
    }
}
