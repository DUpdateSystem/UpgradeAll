package net.xzos.upgradeall.ui.viewmodels.adapters

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data_manager.AppDatabaseManager
import net.xzos.upgradeall.core.server_manager.module.app.App
import net.xzos.upgradeall.ui.activity.detail.AppDetailActivity
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
            if (holder.adapterPosition == NO_POSITION) return@setOnClickListener
            val app = mItemCardViewList.getByHolder(holder).extraData.app
            if (app is App) {
                AppDetailActivity.bundleApp = app
                with(holder.itemCardView.context) {
                    startActivity(Intent(this, AppDetailActivity::class.java))
                }
            }
        }
        holder.itemCardView.setOnLongClickListener { view ->
            if (holder.adapterPosition == NO_POSITION) return@setOnLongClickListener false
            with(mItemCardViewList.getByHolder(holder).extraData.app as App?) {
                if (this != null) {
                    val context = view.context
                    PopupMenu(context, view).let { popupMenu ->
                        popupMenu.menu.let { menu ->
                            // 保存
                            menu.add(R.string.save_as_app).let { menuItem ->
                                menuItem.setOnMenuItemClickListener {
                                    if (runBlocking { AppDatabaseManager.insertAppDatabase(this@with.appDatabase) } != 0L)
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
