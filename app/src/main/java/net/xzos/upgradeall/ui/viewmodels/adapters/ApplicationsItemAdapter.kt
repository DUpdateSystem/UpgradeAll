package net.xzos.upgradeall.ui.viewmodels.adapters

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.ui.activity.MainActivity.Companion.setNavigationItemId
import net.xzos.upgradeall.ui.fragment.AppInfoFragment
import net.xzos.upgradeall.ui.viewmodels.view.holder.CardViewRecyclerViewHolder
import net.xzos.upgradeall.ui.viewmodels.viewmodel.ApplicationsPageViewModel
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.getByHolder

class ApplicationsItemAdapter(
        private val applicationsPageViewModel: ApplicationsPageViewModel
) : AppItemAdapter(applicationsPageViewModel.appCardViewList.value ?: mutableListOf(),
        applicationsPageViewModel.needUpdateAppsLiveData) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewRecyclerViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        holder.hubNameTextView.visibility = View.GONE
        // 单击展开 Release 详情页
        holder.itemCardView.setOnClickListener {
            val app = mItemCardViewList.getByHolder(holder).extraData.app
            if (app is App) {
                AppInfoFragment.bundleApp = app
                setNavigationItemId(R.id.appInfoFragment)
            }
        }
        holder.itemCardView.setOnLongClickListener { view ->
            mItemCardViewList.getByHolder(holder).extraData.app?.run {
                val context = view.context
                PopupMenu(context, view).let { popupMenu ->
                    popupMenu.menu.let { menu ->
                        // 保存
                        menu.add(R.string.save_to_database).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                if (this.appDatabase.save(true))
                                    MiscellaneousUtils.showToast(R.string.save_successfully, Toast.LENGTH_SHORT)
                                return@setOnMenuItemClickListener true
                            }
                        }
                        // 保存到分组
                        menu.add(R.string.add_to_group).let { menuItem ->
                            menuItem.setOnMenuItemClickListener {
                                showSelectGroupPopMenu(view, holder)
                                return@setOnMenuItemClickListener true
                            }
                        }
                        popupMenu.show()
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
                val tabInfoList = applicationsPageViewModel.getTabIndexList()
                for ((tabIndex, tabInfo) in tabInfoList)
                    menu.add(tabInfo.name).let { menuItem: MenuItem ->
                        menuItem.setOnMenuItemClickListener {
                            if (applicationsPageViewModel.addItemToTabPage(holder.adapterPosition, tabIndex))
                                MiscellaneousUtils.showToast(R.string.save_successfully, Toast.LENGTH_SHORT)
                            return@setOnMenuItemClickListener true
                        }
                    }
            }
            popupMenu.show()
        }
    }
}
